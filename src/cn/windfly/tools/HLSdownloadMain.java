package cn.windfly.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.windfly.util.EncodeUtil;
import cn.windfly.util.FileUtil;
import cn.windfly.util.HttpUtil;
import cn.windfly.util.NumberByteUtil;
import cn.windfly.util.ValidateUtil;

public class HLSdownloadMain {

	public static void main(String[] args) throws Exception {
		// 斗鱼测试通过
		downloadM3u8(
				"https://videows1-txcdn.douyucdn.cn/live/high_5383679920200204185729-upload-8d6e/playlist.m3u8?tlink=5e4931e2&tplay=5e49be82&exper=0&nlimit=5&us=8c0871e1c2b50d977954f2ca00011501&sign=d8311bab8eb7645f2aae1f25cdcc2d2d&u=0&d=8c0871e1c2b50d977954f2ca00011501&ct=web&vid=12683936&pt=2&cdn=tx",
				"H:\\PRIVATE\\JAVA\\HLSDownload\\download");
		
//		downloadM3u8(
//				"https://1252524126.vod2.myqcloud.com/9764a7a5vodtransgzp1252524126/ac26cd695285890787137090023/drm/v.f230.m3u8",
//				"H:\\PRIVATE\\JAVA\\HLSDownload\\download");

	}

	static String url;
	static String keyUri;
	static byte[] keyByte;
	static String destPath = ".";
	static List<String> m3u8Content;
	static int num = 0;
	static int donenum = 0;
	static int limittest = Integer.MAX_VALUE;
//	static int limittest = 5;
	static String iv = "0x00000000000000000000000000000000";
	static ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(10);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static List<String> tsPaths = new ArrayList();
	static boolean useKey=false;
	static String encodeMethod;

	private static void downloadM3u8(String u, String destPaths) throws Exception {
		url = u;
		if (ValidateUtil.isNotEmpty(destPath)) {
			destPath = destPaths;
		}

		getM3u8FileContent();
		if (useKey) {
			getKey();
		}
		downloadTs();
	}

	private static void downloadTs() throws Exception {
		byte[] ivbytes = NumberByteUtil.hexToBytes(iv.replace("0x", ""));

		String upString = url.substring(0, url.lastIndexOf("/") + 1);

		for (int i = 0; i < tsPaths.size(); i++) {
			String tpString = tsPaths.get(i);
			if (!tpString.startsWith("http")) {
				tpString = upString + tpString;
			}

			num++;
			newFixedThreadPool.execute(new MyThread(num, tpString, keyByte, ivbytes, destPath));
			if (num == limittest) {
				break;
			}
		}

	}

	@SuppressWarnings("deprecation")
	private static void getKey() throws UnsupportedEncodingException {
		String decode = URLDecoder.decode(keyUri);
		keyByte = HttpUtil.getBytes(decode);
		System.out.println("keyHex = " + NumberByteUtil.bytesToHex(keyByte));
	}

	private static void getM3u8FileContent() throws URISyntaxException {
		String path = url.toString();
		String string = HttpUtil.getString(path, "utf-8");
		String[] split = string.split("\n");
		for (String string2 : split) {
			if (string2.contains("#EXT-X-KEY")) {
				useKey = true;

				String[] split2 = string2.split(",");
				encodeMethod = split2[0].split(":")[1].split("=")[1].trim();
				keyUri = string2.substring(string2.indexOf("URI=\"") + 5,
						string2.indexOf("\"", string2.indexOf("URI=\"") + 6));
			}
			if (string2.contains("IV=")) {
				iv = string2.substring(string2.indexOf("IV=") + 3);
			}
			if (string2.contains(".ts")) {
				tsPaths.add(string2);
			}

		}
		System.out.println("iv = " + iv);
		System.out.println("keyUri = " + keyUri);
		for (String tsString : tsPaths) {
			System.out.println("tsPaths ==== ");
			System.out.println(tsString);
		}
	}

	public static void done(int n) throws IOException {
		donenum++;
		System.out.println("down " + n + "/" + HLSdownloadMain.tsPaths.size());
		if (donenum == limittest || donenum == tsPaths.size()) {
			String pathString = tsPaths.get(0);
			pathString = ".decode.temp.ts";
			FileOutputStream fileOutputStream = new FileOutputStream(
					destPath + "/decode.mp4");
			for (int i = 1; i <= donenum; i++) {
				fileOutputStream.write(FileUtil.loadFileBytes(destPath + "\\" + i + pathString));
				System.out.println(" 合并 " + i + "/" + donenum);
			}

			fileOutputStream.flush();
			fileOutputStream.close();
			newFixedThreadPool.shutdown();
			for (int i = 1; i <= donenum; i++) {
				new File(destPath + "\\" + i + pathString).delete();
			}

			System.out.println("完成 ");

		}

	}

}

class MyThread extends Thread {
	int num;
	private String pathString;
	private byte[] keyByte;
	private byte[] ivbytes;
	private String destPath;

	public MyThread(int num, String pathString, byte[] keyByte, byte[] ivbytes, String destPath) {
		super();
		this.num = num;
		this.pathString = pathString;
		this.destPath = destPath;
		this.keyByte = keyByte;
		this.ivbytes = ivbytes;
	}

	@Override
	public void run() {

		String fnameString = "\\" + num + ".decode.temp.ts";

		byte[] bytes = HttpUtil.getBytes(pathString);
		if (bytes == null) {
			try {
				throw new Exception(pathString + "  => null");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		byte[] aeSdecode = bytes;
		if (HLSdownloadMain.useKey) {
			if ("NONE".equalsIgnoreCase(HLSdownloadMain.encodeMethod)) {
				aeSdecode = EncodeUtil.AESdecode(bytes, keyByte,null);
			}else if ("AES-128".equalsIgnoreCase(HLSdownloadMain.encodeMethod)) {
				aeSdecode = EncodeUtil.AESdecodeCBCPKCS7Padding(bytes, keyByte, ivbytes);
			}else {
				try {
					throw new Exception("未知加密方法:"+HLSdownloadMain.encodeMethod);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		FileUtil.save(destPath + fnameString, aeSdecode);
		try {
			HLSdownloadMain.done(num);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
