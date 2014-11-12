package cn.xuyao.fastdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerServer;

/**
 * 	图片服务器类
 * 	@author xuyao
 * */
public class ImageServerImpl implements ImageServer {

	private int waitTimes = 2;

	private ConnectionPool pool = null;

	/**
	 * 
	 * @param connnectString
	 *            域名地址或IP
	 * @param port
	 *            端口
	 * @param size
	 *            连接池大小
	 */
	public ImageServerImpl(String[] connnectString, int size, int connect_timeout,
			int network_timeout, String charset) {
		pool = new ConnectionPool(connnectString, size, connect_timeout,
				network_timeout, charset);
	}

	public String uploadFile(File file) throws IOException, Exception {
		return uploadFile(file, getFileExtName(file.getName()));
	}

	public String uploadFile(File file, String suffix) throws IOException,
			Exception {
		byte[] fileBuff = getFileBuffer(file);
		return send(fileBuff, suffix);
	}

	public String uploadFile(byte[] fileBuff, String suffix)
			throws IOException, Exception {
		return send(fileBuff, suffix);
	}

	private String send(byte[] fileBuff, String fileExtName)
			throws IOException, Exception {
		String upPath = null;
		TrackerServer trackerServer = null;
		try {
			trackerServer = pool.checkout(waitTimes);

			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(trackerServer,
					storageServer);
			upPath = client1.upload_file1(fileBuff, fileExtName, null);
			pool.checkin(trackerServer);
		} catch (InterruptedException e) {
			// 确实没有空闲连接,并不需要删除与fastdfs连接
			throw e;
		} catch (NullPointerException e) {
			throw e;
		} catch (Exception e) {
			// 发生io异常等其它异常，默认删除这次连接重新申请
			e.printStackTrace();
			pool.drop(trackerServer);
			throw e;
		}
		return upPath;
	}

	private String getFileExtName(String name) {
		String extName = null;
		if (name != null && name.contains(".")) {
			extName = name.substring(name.lastIndexOf(".") + 1);
		}
		return extName;
	}

	private byte[] getFileBuffer(File file) {
		byte[] fileByte = null;
		try {
			FileInputStream fis = new FileInputStream(file);
			fileByte = new byte[fis.available()];
			fis.read(fileByte);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileByte;
	}


	public int getWaitTimes() {
		return waitTimes;
	}

	public void setWaitTimes(int waitTimes) {
		this.waitTimes = waitTimes;
	}

	@Override
	public boolean deleteFile(String fileId) throws IOException, Exception {
		boolean result=false;
		TrackerServer trackerServer =null;
		try {
			trackerServer = pool.checkout(waitTimes);
			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(trackerServer, storageServer);
			result=client1.delete_file1(fileId)==0?true:false;
			pool.checkin(trackerServer);
		} catch (Exception e) {
			pool.drop(trackerServer);
			e.printStackTrace();
			throw e;
		}
		return result;
	}

	@Override
	public byte[] getFileByID(String fileId)throws IOException,Exception{
		byte[] result=null;
		TrackerServer trackerServer =null;
		try {
			trackerServer = pool.checkout(waitTimes);
			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(trackerServer, storageServer);
			result=client1.download_file1(fileId);
			pool.checkin(trackerServer);
		} catch (Exception e) {
			pool.drop(trackerServer);
			e.printStackTrace();
			throw e;
		}
		return result;
	}
}
