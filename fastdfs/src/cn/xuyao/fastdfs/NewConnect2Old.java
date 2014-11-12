package cn.xuyao.fastdfs;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerServer;
@Deprecated
public class NewConnect2Old {
	public static void olds(ArrayBlockingQueue<TrackerServer> idleConnectionPool ){
		if(idleConnectionPool.size()>0){
			for(int i=0;i<idleConnectionPool.size();i++){
				TrackerServer ts=idleConnectionPool.poll();
				idleConnectionPool.add(ts);
			}
		}
	}
	
	public static void old(TrackerServer ts){
		try {
			byte[] fileBuff=new byte[]{1,2,3,1};
			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(ts,
					storageServer);
			String upPath = client1.upload_file1(fileBuff, fileExtName, null);
			client1.delete_file1(upPath);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static String fileExtName="tmp";
}
