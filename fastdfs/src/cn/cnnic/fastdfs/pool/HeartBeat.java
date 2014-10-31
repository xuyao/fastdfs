package cn.cnnic.fastdfs.pool;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.csource.fastdfs.TrackerServer;

public class HeartBeat {
	
	private ConnectionPool pool = null;
	
	public HeartBeat(ConnectionPool pool){
		this.pool=pool;
	}

	public void beat(){
		TimerTask task=new TimerTask() {
			@Override
			public void run() {
				ArrayBlockingQueue<TrackerServer> idleConnectionPool=pool.getIdleConnectionPool();
				TrackerServer ts=null;
				ImageServerPoolSysout.warn("ConnectionPool execution HeartBeat to fdfs server");
				for(int i=0;i<idleConnectionPool.size();i++){
					try {
						ts=idleConnectionPool.poll(waitTimes,
								TimeUnit.SECONDS);
						if(ts!=null){
							org.csource.fastdfs.ProtoCommon.activeTest(ts.getSocket());
							idleConnectionPool.add(ts);
						}else{
							//代表已经没有空闲长连接
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						//发生异常,要删除，进行重建
						pool.drop(ts);
						e.printStackTrace();
					}finally{
					}
				}
			}
		};
		Timer timer=new Timer();
		timer.schedule(task, ahour, ahour);
	}
	public static int ahour=1000*60*60*1;
	public static int waitTimes=0;
	
}
