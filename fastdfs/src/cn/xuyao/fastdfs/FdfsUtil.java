package cn.xuyao.fastdfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 	FastDFS文件上传下载接口
 * 	@author xuyao
 * */
public class FdfsUtil {

	private static final Logger logger = LoggerFactory.getLogger(FdfsUtil.class);

	private static String configName = "fdfs.properties";
	
	private static ImageServer imageServer = null;

	static {
		try {
			// Configuration file of the FastDFS to initialize
			InputStream is = FdfsUtil.class.getResourceAsStream("/" + configName);
			Properties p = new Properties();
			p.load(is);
			String servers = p.getProperty("fdfs.tracker_server");
			String poolSize = p.getProperty("fdfs.pool.size");
			String[] server_arr = servers.split(",");
			String connect_timeout = p.getProperty("fdfs.connect_timeout");
			String network_timeout = p.getProperty("fdfs.network_timeout");
			String charset = p.getProperty("fdfs.charset");
			imageServer = new ImageServerImpl(server_arr, Integer.parseInt(poolSize), Integer.parseInt(connect_timeout), 
					Integer.parseInt(network_timeout), charset);
			logger.info("初始化文件服务fastdfs客户端");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 	retrun imageserver
	 * */
	public static ImageServer getImageServer(){
		return imageServer;
	}

}
