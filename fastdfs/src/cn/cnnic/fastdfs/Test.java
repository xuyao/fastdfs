package cn.cnnic.fastdfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		upload();
		download();
	}
	
	
	public static void download(){
		ImageServer is = FdfsUtil.getImageServer();
		byte[] b = null;
		try {
			b = is.getFileByID("group1/M00/02/09/2vFspFRQU0aAJk1GAAAFblK6nss187.txt");
			File f = new File("e:\\3.txt");
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(b);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	
	public static void upload(){
		long a = System.currentTimeMillis();
		ImageServer is = FdfsUtil.getImageServer();
		try {
			for(int i=0;i<10000;i++){
				System.out.println(i+"   "+is.uploadFile(new File("e:\\2.txt")));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - a);
		System.exit(0);
	}

}
