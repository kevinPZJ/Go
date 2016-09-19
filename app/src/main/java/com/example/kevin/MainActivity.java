package com.example.kevin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.example.kevin.MyOrientationListener.OnOrientationListener;
import com.example.kevin.view.ArcMenu;
import java.text.DecimalFormat;


public class MainActivity extends Activity
{
	private TextView tv_show_step;// 显示步数
	private TextView tv_timer;// 显示路程
	private TextView tv_distance;//  显示路程
    private TextView step_counter;

	private Button btn_start;// 开始按钮
	private Button btn_stop;// 暂停/清零按钮

	private long timer = 0;//运动时间
	private  long startTimer = 0;// 开始时间

	private  long tempTime = 0;

	private Double distance = 0.0;// 距离


	private int step_length = 0;  //步长
	private int weight = 0;       //体重
	private int total_step = 0;   //走的总步数

	private Thread thread;  //定义线程对象


	//百度地图相关变量

	private MapView mMapView;
	private BaiduMap mBaiduMap;

	private Context context;


	// 定位相关
	private LocationClient mLocationClient;
	private MyLocationListener mLocationListener;
	private boolean isFirstIn = true;  // 判断是否第一次定位
	private double mLatitude;
	private double mLongtitude;
	// 自定义定位图标
	private BitmapDescriptor mIconLocation;
	private MyOrientationListener myOrientationListener;
	private float mCurrentX;
	private LocationMode mLocationMode;

	private ArcMenu mArcMenu;//自定义卫星菜单
	private PowerManager.WakeLock mWakeLock;
	private PowerManager mPowerManager;// 电源管理服务

	private long exitTime = 0;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 在使用SDK各组件之前初始化context信息，传入ApplicationContext
		// 注意该方法要再setContentView方法之前实现
		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.activity_main);

		this.context = this;
		if (SettingsActivity.sharedPreferences == null) {
			SettingsActivity.sharedPreferences = this.getSharedPreferences(
					SettingsActivity.SETP_SHARED_PREFERENCES,
					Context.MODE_PRIVATE);
		}
		init_mapView();
		// 初始化定位
		initLocation();
//		initMarker();
		initEvent();// 卫星菜单的监听事件


		if (thread == null) {
			thread = new Thread() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					super.run();
					int temp = 0;
					while (true) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (StepCounterService.FLAG) {
							Message msg = new Message();
							if (temp != StepDetector.CURRENT_SETP) {
								temp = StepDetector.CURRENT_SETP;
							}
							if (startTimer != System.currentTimeMillis()) {
								timer = tempTime + System.currentTimeMillis()
										- startTimer;
							}
							handler.sendMessage(msg);//
						}
					}
				}
			};
			thread.start();
		}


		mBaiduMap.setOnMarkerClickListener(new OnMarkerClickListener()
		{
			@Override
			public boolean onMarkerClick(Marker marker)
			{
				Bundle extraInfo = marker.getExtraInfo();
				TextView tv = new TextView(context);
				tv.setBackgroundResource(R.drawable.location_tips);
				tv.setPadding(30, 20, 30, 50);
				tv.setTextColor(Color.parseColor("#ffffff"));
				final LatLng latLng = marker.getPosition();
				Point p = mBaiduMap.getProjection().toScreenLocation(latLng);
				p.y -= 47;
				LatLng ll = mBaiduMap.getProjection().fromScreenLocation(p);
				return true;
			}
		});
		mBaiduMap.setOnMapClickListener(new OnMapClickListener()
		{

			@Override
			public boolean onMapPoiClick(MapPoi arg0)
			{
				return false;
			}

			@Override
			public void onMapClick(LatLng arg0)
			{
//				mMarkerLy.setVisibility(View.GONE);
//				mBaiduMap.hideInfoWindow();
			}
		});
	}


	Handler handler = new Handler() {

		@Override                      //这个方法是从父类/接口 继承过来的，需要重写一次
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);        // 此处可以更新UI
//			acquireWakeLock();
			countDistance();     //调用距离计算方法
			countStep();          //调用步数计算方法

			try {

				tv_show_step.setText(total_step + "");// 显示当前步数
				tv_distance.setText(formatDouble(distance));
				tv_timer.setText(getFormatTime(timer));// 显示当前运行时间

			} catch (Exception e) { //

			}





		}

	};



	private void initLocation()
	{

		mLocationMode = LocationMode.NORMAL; //普通地图模式
		mLocationClient = new LocationClient(this);
		mLocationListener = new MyLocationListener();
		mLocationClient.registerLocationListener(mLocationListener); //注册定位监听器

		LocationClientOption option = new LocationClientOption(); //百度地图的相关设定
		option.setCoorType("bd09ll");
		option.setIsNeedAddress(true);  //地址
		option.setOpenGps(true);		//开启GPS
		option.setScanSpan(1000);		//请求间隔1000ms
		mLocationClient.setLocOption(option);
		// 初始化图标
		mIconLocation = BitmapDescriptorFactory
				.fromResource(R.drawable.navi_map_gps_locked);
		myOrientationListener = new MyOrientationListener(context);

		myOrientationListener
				.setOnOrientationListener(new OnOrientationListener()
				{
					@Override
					public void onOrientationChanged(float x)
					{
						mCurrentX = x;
					}
				});

	}

	private void init_mapView()
	{
		mMapView = (MapView) findViewById(R.id.id_bmapView);//百度地图实例化
		mBaiduMap = mMapView.getMap();//获取地图
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(20.0f);//定位精确度 （10米）
		mBaiduMap.setMapStatus(msu);

		mArcMenu = (ArcMenu) findViewById(R.id.id_menu); //卫星菜单
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
//		acquireWakeLock();
		mMapView.onResume();
		addViewAgain();
		init_step(); //步数计算相关
	}

	@Override
	protected void onStart()
	{
		super.onStart();
//		acquireWakeLock();
		// 开启定位
		mBaiduMap.setMyLocationEnabled(true);
		if (!mLocationClient.isStarted())
			mLocationClient.start();
		// 开启方向传感器
		myOrientationListener.start();
	}

	@Override
	protected void onPause()
	{

		super.onPause();
		// 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
		mMapView.onPause();
//		acquireWakeLock();
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		// 停止定位
		mBaiduMap.setMyLocationEnabled(false);
		mLocationClient.stop();
		// 停止方向传感器
		myOrientationListener.stop();

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		// 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
		mMapView.onDestroy();
//		releaseWakeLock();
	}




	/**
	 * 添加覆盖物
	 * 
	 * @param infos
	 */


	/**
	 * 定位到我的位置
	 */
	private void centerToMyLocation()  ///定位当前位置，中心点
	{
		LatLng latLng = new LatLng(mLatitude, mLongtitude);
		MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
		mBaiduMap.animateMapStatus(msu);
	}

	private class MyLocationListener implements BDLocationListener
	{
		@Override
		public void onReceiveLocation(BDLocation location)
		{

			MyLocationData data = new MyLocationData.Builder()//
					.direction(mCurrentX)//
					.accuracy(location.getRadius())//
					.latitude(location.getLatitude())//
					.longitude(location.getLongitude())//
					.build();
			mBaiduMap.setMyLocationData(data);
			// 设置自定义图标
			MyLocationConfiguration config = new MyLocationConfiguration(
					mLocationMode, true, mIconLocation);
			mBaiduMap.setMyLocationConfigeration(config);

			// 更新经纬度
			mLatitude = location.getLatitude();
			mLongtitude = location.getLongitude();

			if (isFirstIn)
			{
				LatLng latLng = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
				mBaiduMap.animateMapStatus(msu);
				isFirstIn = false;

				Toast.makeText(context, location.getAddrStr(),
						Toast.LENGTH_SHORT).show();
			}

		}
	}
	private void addView() {
		tv_show_step = (TextView) this.findViewById(R.id.show_step);
		tv_timer = (TextView) this.findViewById(R.id.timer);

    	tv_distance = (TextView) this.findViewById(R.id.distance);

		btn_start = (Button) this.findViewById(R.id.start);
		btn_stop = (Button) this.findViewById(R.id.stop);

		step_counter = (TextView)findViewById(R.id.step_counter);
		step_counter.setText("steps");


		Intent service = new Intent(this, StepCounterService.class);
		stopService(service);
		StepDetector.CURRENT_SETP = 0;
		tempTime = timer = 0;
		tv_timer.setText(getFormatTime(timer));      //tempTime记录运动的总时间，timer记录每次运动时间
		tv_show_step.setText("0");

		tv_distance.setText(formatDouble(0.0));


		handler.removeCallbacks(thread);

	}
	private void addViewAgain() {
		tv_show_step = (TextView) this.findViewById(R.id.show_step);
		tv_timer = (TextView) this.findViewById(R.id.timer);

		tv_distance = (TextView) this.findViewById(R.id.distance);

		btn_start = (Button) this.findViewById(R.id.start);
		btn_stop = (Button) this.findViewById(R.id.stop);

		step_counter = (TextView)findViewById(R.id.step_counter);
		step_counter.setText("steps");

		StepDetector.CURRENT_SETP = total_step;
		tv_show_step.setText(total_step + "");
		tv_distance.setText(formatDouble(distance));
		tv_timer.setText(getFormatTime(timer));// 显示当前运行时间
		handler.removeCallbacks(thread);

	}

	/**
	 * 初始化计算器界面
	 */
	private void init_step() {

		step_length = SettingsActivity.sharedPreferences.getInt(
				SettingsActivity.STEP_LENGTH_VALUE, 70);
		weight = SettingsActivity.sharedPreferences.getInt(
				SettingsActivity.WEIGHT_VALUE, 50);

		countDistance();
		countStep();

		tv_timer.setText(getFormatTime(timer + tempTime));

		tv_distance.setText(formatDouble(distance));

		tv_show_step.setText(total_step + "");

//		btn_start.setEnabled(!StepCounterService.FLAG);
//		btn_stop.setEnabled(StepCounterService.FLAG);

		if (StepCounterService.FLAG) {
			btn_stop.setText(getString(R.string.pause));
		} else if (StepDetector.CURRENT_SETP > 0) {
			btn_stop.setEnabled(true);
			btn_stop.setText(getString(R.string.cancel));
		}
//

	}
	private String formatDouble(Double doubles) {
		DecimalFormat format = new DecimalFormat("####.##");
		String distanceStr = format.format(doubles);
		return distanceStr.equals(getString(R.string.zero)) ? getString(R.string.double_zero)
				: distanceStr;
	}

	public void onClick(View view) {
		Intent service = new Intent(this, StepCounterService.class);
		switch (view.getId()) {
			case R.id.start:

				startService(service);
				btn_start.setEnabled(false);
				btn_stop.setEnabled(true);
				btn_stop.setText(getString(R.string.pause));
				startTimer = System.currentTimeMillis();
				tempTime = timer;
				break;

			case R.id.stop:
				stopService(service);

				if (StepCounterService.FLAG && StepDetector.CURRENT_SETP > 0) {
					btn_stop.setText(getString(R.string.cancel));
				} else {
					StepDetector.CURRENT_SETP = 0;
					tempTime = timer = 0;

					btn_stop.setText(getString(R.string.pause));
					btn_stop.setEnabled(false);

					tv_timer.setText(getFormatTime(timer));

					tv_show_step.setText("0");
					tv_distance.setText(formatDouble(0.0));

					handler.removeCallbacks(thread);
				}
				btn_start.setEnabled(true);
				break;
		}
	}

    /**
     * 计算并格式化doubles数值，保留两位有效数字
     *
     * @param doubles
     * @return 返回当前路程
     */
	private String getFormatTime(long time) {
		time = time / 1000;
		long second = time % 60;
		long minute = (time % 3600) / 60;
		long hour = time / 3600;

		// ???????????λ
		// String strMillisecond = "" + (millisecond / 10);
		// ???????λ
		String strSecond = ("00" + second)
				.substring(("00" + second).length() - 2);
		// ???????λ
		String strMinute = ("00" + minute)
				.substring(("00" + minute).length() - 2);
		// ??????λ
		String strHour = ("00" + hour).substring(("00" + hour).length() - 2);

		return strHour + ":" + strMinute + ":" + strSecond;
		// + strMillisecond;
	}

	private void countDistance() {
		if (StepDetector.CURRENT_SETP % 2 == 0) {
			distance = (StepDetector.CURRENT_SETP / 2) * 3 * step_length * 0.01;
		} else {
			distance = ((StepDetector.CURRENT_SETP / 2) * 3 + 1) * step_length * 0.01;
		}
	}


	private void countStep() {
		if (StepDetector.CURRENT_SETP % 2 == 0) {
			total_step = StepDetector.CURRENT_SETP;
		} else {
			total_step = StepDetector.CURRENT_SETP +1;
		}

		total_step = StepDetector.CURRENT_SETP;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}

	private void initEvent()
	{

		if (mArcMenu.isOpen())
		{	mArcMenu.toggleMenu(600);}

		mArcMenu.setOnMenuItemClickListener(new ArcMenu.OnMenuItemClickListener()
		{
			@Override
			public void onClick(View view, int pos)
			{
//				Toast.makeText(MainActivity.this, pos+":"+view.getTag(), Toast.LENGTH_SHORT).show();

				switch (pos){
					case 1 :
						Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
						startActivity(intent);
						break;
					case 2 :
						centerToMyLocation();
						Toast.makeText(MainActivity.this, "定位到当前位置", Toast.LENGTH_SHORT).show();
						break;
					case 3 :
						mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
						Toast.makeText(MainActivity.this, "已开启卫星地图", Toast.LENGTH_SHORT).show();
						break;
					case 4 :
						mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
						Toast.makeText(MainActivity.this, "已切换普通地图", Toast.LENGTH_SHORT).show();
						break;
					case 5 :
						if (mBaiduMap.isTrafficEnabled())
						{
							mBaiduMap.setTrafficEnabled(false);
							Toast.makeText(MainActivity.this, "实时交通已关闭", Toast.LENGTH_SHORT).show();
						} else
						{
							mBaiduMap.setTrafficEnabled(true);
							Toast.makeText(MainActivity.this, "开启实时交通", Toast.LENGTH_SHORT).show();
						}

						break;
					default:
						break;




				}
			}
		});
	}

//	private void acquireWakeLock()
//	{
//		if (null == mWakeLock)
//		{
//			mPowerManager = (PowerManager) this
//					.getSystemService(Context.POWER_SERVICE);
//			mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
//					| PowerManager.ACQUIRE_CAUSES_WAKEUP, "S");
//			mWakeLock.acquire();
//		}
//			if (null != mWakeLock)
//			{
//				mWakeLock.acquire();
//			}
//		}
//
//	private void releaseWakeLock()
//	{
//		if (null != mWakeLock)
//		{
//			mWakeLock.release();
//			mWakeLock = null;
//		}
//	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}



