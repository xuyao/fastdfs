package cn.cnnic.fastdfs.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerGroup;
import org.csource.fastdfs.TrackerServer;

/**
 * 	连接池类
 *  @author xuyao
 * */
public class ConnectionPool {

	// busy connection instances
	private ConcurrentHashMap<TrackerServer, Object> busyConnectionPool = null;
	// idle connection instances
	private ArrayBlockingQueue<TrackerServer> idleConnectionPool = null;
	// delay lock for initialization

	// the connection string for ip
	private String[] tgStr = null;
	
	//the poolsize for connection
	private int poolSize = 10;
	
	private int connect_timeout = 2000;
	
	private int network_timeout=30000;
	
	private String charset = "utf-8";
	
	private Object obj = new Object();
	
	//heart beat
	HeartBeat beat=null;

	public ConnectionPool(String[] tgStr, int size, int connect_timeout,
			int network_timeout, String charset) {
		this.tgStr = tgStr;
		this.poolSize = size;
		this.connect_timeout = connect_timeout;
		this.network_timeout = network_timeout;
		this.charset = charset;
		init();
		beat=new HeartBeat(this);
		beat.beat();
	}

	/**
	 * init the connection pool
	 * 
	 * @param size
	 */
	private void init() {
		initClientGlobal();
		busyConnectionPool = new ConcurrentHashMap<TrackerServer, Object>();
		idleConnectionPool = new ArrayBlockingQueue<TrackerServer>(this.poolSize);
		TrackerServer trackerServer = null;
		try {
			TrackerClient trackerClient = new TrackerClient();
			for (int i = 0; i < poolSize; i++) {
				trackerServer = trackerClient.getConnection();
				org.csource.fastdfs.ProtoCommon.activeTest(trackerServer.getSocket());
				idleConnectionPool.add(trackerServer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (trackerServer != null) {
				try {
					trackerServer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 1. pop one connection from the idleConnectionPool,
	// 2. push the connection into busyConnectionPool;
	// 3. return the connection
	// 4. if no idle connection, do wait for wait_time seconds, and check again
	public TrackerServer checkout(int waitTimes) throws InterruptedException {
		TrackerServer client1 = idleConnectionPool.poll(waitTimes,
				TimeUnit.SECONDS);
		if (client1 == null) {
			ImageServerPoolSysout
					.warn("ImageServerPool wait time out ,return null");
			throw new NullPointerException(
					"ImageServerPool wait time out ,return null");
		}
		busyConnectionPool.put(client1, obj);
		return client1;
	}

	// 1. pop the connection from busyConnectionPool;
	// 2. push the connection into idleConnectionPool;
	// 3. do nessary cleanup works.
	public void checkin(TrackerServer client1) {
		if (busyConnectionPool.remove(client1) != null) {
			idleConnectionPool.add(client1);
		}
	}

	// so if the connection was broken due to some erros (like
	// : socket init failure, network broken etc), drop this connection
	// from the busyConnectionPool, and init one new connection.
	public synchronized void drop(TrackerServer trackerServer) {
		// first less connection
		try {
			trackerServer.close();
		} catch (IOException e1) {
		}
		if (busyConnectionPool.remove(trackerServer) != null) {
			try {
				ImageServerPoolSysout
						.warn("ImageServerPool drop a connnection");
				ImageServerPoolSysout.warn("ImageServerPool size:"
						+ (busyConnectionPool.size() + idleConnectionPool
								.size()));
				TrackerClient trackerClient = new TrackerClient();
				trackerServer = trackerClient.getConnection();
			} catch (IOException e) {
				trackerServer = null;
				ImageServerPoolSysout
						.warn("ImageServerPool getConnection generate exception");
				e.printStackTrace();
			} finally {
				if(!isContinued(trackerServer)){
					return;
				}
				try {
					org.csource.fastdfs.ProtoCommon.activeTest(trackerServer.getSocket());
					idleConnectionPool.add(trackerServer);
					ImageServerPoolSysout.warn("ImageServerPool add a connnection");
					ImageServerPoolSysout.warn("ImageServerPool size:"
							+ (busyConnectionPool.size() + idleConnectionPool
									.size()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public boolean isContinued(TrackerServer trackerServer){
		if (trackerServer == null && hasConnectionException) {
			return false;
		}
		if (trackerServer == null) {
			hasConnectionException = true;
			// only a thread;
			detector();
		}
		if (hasConnectionException) {
			return false;
		}
		return true;
	}

	private void detector() {

		new Thread() {
			@Override
			public void run() {
				String msg="detector connection fail to "+tgStr;
				while (true) {
					TrackerServer trackerServer = null;
					TrackerClient trackerClient = new TrackerClient();
					try {
						trackerServer = trackerClient.getConnection();
						Thread.sleep(5000);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}finally{
						if(trackerServer!=null){
							msg="detector connection success to "+tgStr;
							break;
						}
						ImageServerPoolSysout.warn("current ImageServerPool has size:"
								+ (busyConnectionPool.size() + idleConnectionPool
										.size()));
						ImageServerPoolSysout.warn(msg);
					}
				}
				ImageServerPoolSysout.warn(msg);
				
//				//close before tracker server
//				if(busyConnectionPool.size()!=0){
//					ImageServerPoolSysout.warn("busyConnectionPool start close trackerserver");
//					for(Entry<TrackerServer, Object> entry:busyConnectionPool.entrySet()){
//						try {
//							entry.getKey().close();
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}
				
				if(idleConnectionPool.size()!=0){
					ImageServerPoolSysout.warn("idleConnectionPool start close trackerserver");
					ImageServerPoolSysout.warn(msg);

					for(int i=0;i<poolSize;i++){
						TrackerServer ts=idleConnectionPool.poll();
						if(ts!=null){
							try {
								ts.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				//re init
				hasConnectionException=false;
				init();
			}
		}.start();
	}

	boolean hasConnectionException = false;

	//初始化操作
	private void initClientGlobal() {
		int num = tgStr.length;
		InetSocketAddress[] trackerServers = new InetSocketAddress[num];
		for(int i=0;i<num;i++){
			String[] ip_port = tgStr[i].split(":");
			trackerServers[i] = new InetSocketAddress(ip_port[0], Integer.parseInt(ip_port[1]));
		}
		ClientGlobal.setG_tracker_group(new TrackerGroup(trackerServers));
		ClientGlobal.setG_connect_timeout(connect_timeout);
		ClientGlobal.setG_network_timeout(network_timeout);
		ClientGlobal.setG_anti_steal_token(false);
		ClientGlobal.setG_charset(charset);
		ClientGlobal.setG_secret_key(null);
	}

	public ArrayBlockingQueue<TrackerServer> getIdleConnectionPool() {
		return idleConnectionPool;
	}

	
	
	
}
