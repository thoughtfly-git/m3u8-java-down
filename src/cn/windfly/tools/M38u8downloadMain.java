package cn.windfly.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.windfly.util.EncodeUtil;
import cn.windfly.util.FileUtil;
import cn.windfly.util.HttpUtil;
import cn.windfly.util.JsonUtil;
import cn.windfly.util.LogUtil;
import cn.windfly.util.NumberByteUtil;
import cn.windfly.util.ValidateUtil;

public class M38u8downloadMain {

	public static void main(String[] args) throws Exception {

//		downloadM3u8("https://edu.aliyun.com/hls/5058/stream/sd/bXyaiFBmzWyQtkuk2zQWcKJhEc7kVFGm.m3u8?courseId=838",
//				"H:\\PRIVATE\\JAVA\\HLSDownload\\download");

		// 斗鱼测试通过
		downloadM3u8(
				"https://videows1-txcdn.douyucdn.cn/live/high_5383679920200204185729-upload-8d6e/playlist.m3u8?tlink=5e4a7532&tplay=5e4b01d2&exper=0&nlimit=5&us=a1fcf0d41e8a260b294299fd00091501&sign=40433b5b04537a167df326b4299b6c40&u=0&d=a1fcf0d41e8a260b294299fd00091501&ct=web&vid=12683936&pt=2&cdn=tx",
				"H:\\PRIVATE\\JAVA\\HLSDownload\\download");
	}

	static String destPath = ".";
	static String url;
	static int donenum = 0;
	static int limittest = Integer.MAX_VALUE;
	static ArrayList<Item> items = new ArrayList<Item>();
//	static int limittest = 5;

	static ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(10);
	private static boolean isQiQiuYun = false;

	private static void downloadM3u8(String u, String destPaths) throws Exception {
		url = u;
		if (ValidateUtil.isNotEmpty(destPath)) {
			destPath = destPaths;
		}
		getM3u8FileContent();
		downloadTs();
	}

	@SuppressWarnings("deprecation")
	private static void getM3u8FileContent() throws URISyntaxException {
		String path = url.toString();
		String string = HttpUtil.getString(path, "utf-8");

		if (string.contains("qiqiuyun.net/") || string.contains("aliyunedu.net/")
				|| string.contains("qncdn.edusoho.net/")) {
			// 气球云
			isQiQiuYun = true;
		}

		String[] split = string.split("\n");
		LogUtil.info(string);

		boolean useKey = false;
		String encodeMethod = null;
		String keyUri = null;
		String oldKeyStr = null;
		byte[] keyByte = null;
		String iv = null;
		for (int i = 0; i < split.length; i++) {
			String eachline = split[i];
			if (eachline.startsWith("#EXT-X-KEY")) {
				useKey = true;
				String[] split2 = eachline.split(",");
				encodeMethod = split2[0].split(":")[1].split("=")[1].trim();
				String keyUri_temp = eachline.substring(eachline.indexOf("URI=\"") + 5,
						eachline.indexOf("\"", eachline.indexOf("URI=\"") + 6));
				if (!keyUri_temp.equals(keyUri)) {
					keyUri = keyUri_temp;
					String decode = URLDecoder.decode(keyUri);
					keyByte = HttpUtil.getBytes(decode);
					oldKeyStr = new String(keyByte);

					keyByte = parseKey(keyByte);

					System.out.println("newkeystr = " + new String(keyByte));
				}
				iv = eachline.substring(eachline.indexOf("IV=") + 3);
			}
			if (eachline.contains("#EXT-X-DISCONTINUITY")) {
			}
			if (eachline.contains(".ts")) {

				if (useKey) {

				}

				Item item = new Item(items.size() + 1, keyUri, oldKeyStr, keyByte, iv, eachline, useKey, encodeMethod);
				items.add(item);
				encodeMethod = null;
				iv = null;
				useKey = false;
				LogUtil.info(JsonUtil.toJson(item));
				if (items.size() == limittest) {
					break;
				}
			}

		}
		System.out.println("ts ready to download==== ");
	}

	private static byte[] parseKey(byte[] keyByte) {
		if (isQiQiuYun) {
			String[] indexs = "0-1-2-3-4-5-6-7-8-10-11-12-14-15-16-18".split("-");
			if (keyByte.length == 20) {
				int algorithmCharCode = keyByte[0];
				char algorithmChar = (char) algorithmCharCode;
				int algorithmCharStart = Integer.parseInt("" + algorithmChar, 36) % 7;
				int firstAlgorithmCharCode = keyByte[algorithmCharStart];
				char firstAlgorithmChar = (char) firstAlgorithmCharCode;
				int secondAlgorithmCharCode = keyByte[algorithmCharStart + 1];
				char secondAlgorithmChar = (char) secondAlgorithmCharCode;
				int algorithmNum = Integer.parseInt("" + firstAlgorithmChar + secondAlgorithmChar, 36) % 3;

				if (algorithmNum == 1) {
					indexs = "0-1-2-3-4-5-6-7-18-16-15-13-12-11-10-8".split("-");
				} else if (algorithmNum == 0) {
					indexs = "0-1-2-3-4-5-6-7-8-10-11-12-14-15-16-18".split("-");
				} else if (algorithmNum == 2) {
					int a_CODE = 'a';

					int c9 = keyByte[8];
					int c9t = keyByte[9];
					int c10 = keyByte[10];
					int c10t = keyByte[11];
					int c14 = keyByte[15];
					int c14t = keyByte[16];
					int c15 = keyByte[17];
					int c15t = keyByte[18];

					int c9r = c9 - a_CODE + (c9t + 1) * 26 - a_CODE;
					int c10r = c10 - a_CODE + (c10t + 1) * 26 - a_CODE;
					int c14r = c14 - a_CODE + (c14t + 1) * 26 - a_CODE;
					int c15r = c15 - a_CODE + (c15t + 2) * 26 - a_CODE;

					keyByte[8] = (byte) c9r;
					keyByte[9] = (byte) c10r;
					keyByte[10] = keyByte[12];
					keyByte[11] = keyByte[13];
					keyByte[12] = keyByte[14];
					keyByte[13] = (byte) c14r;
					keyByte[14] = (byte) c15r;
					keyByte[15] = keyByte[19];
				}
			} else if (keyByte.length == 17) {
				indexs = "1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16".split("-");
			} else {
				indexs = "0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15".split("-");
			}

			byte[] newkeybyte = new byte[16];
			for (int j = 0; j < indexs.length; j++) {
				int parseInt = Integer.parseInt(indexs[j]);
				if (j < 16) {
					newkeybyte[j] = keyByte[parseInt];
				}
			}
			keyByte = newkeybyte;

		}
		return keyByte;
	}

	private static void downloadTs() throws Exception {

		String upString = url.substring(0, url.lastIndexOf("/") + 1);

		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (!item.tsPath.startsWith("http")) {
				item.tsPath = upString + item.tsPath;
			}

			newFixedThreadPool.execute(new MyThread(item, destPath));
			if (i + 1 == limittest) {
				break;
			}
		}

	}

	public static void done(int n) throws IOException {
		donenum++;
		System.out.println("down " + n + "/" + M38u8downloadMain.items.size());
		if (donenum == limittest || donenum == items.size()) {
			String pathString = ".decode.temp.ts";
			FileOutputStream fileOutputStream = new FileOutputStream(destPath + "/decode.mp4");
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
	private String destPath;
	Item item;

	public MyThread(Item item, String destPath2) {
		this.destPath = destPath2;
		this.item = item;
	}

	@Override
	public void run() {
		int num = item.num;
		String pathString = item.tsPath;
		byte[] keyByte = item.keyByte;
		byte[] ivbytes = item.iv == null ? null : NumberByteUtil.hexToBytes(item.iv.replace("0x", ""));

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
		FileUtil.save(destPath + "\\" + num + ".temp.ts", aeSdecode);
		if (item.useKey) {
			if ("NONE".equalsIgnoreCase(item.encodeMethod)) {
				aeSdecode = EncodeUtil.AESdecode(bytes, keyByte, null);
			} else if ("AES-128".equalsIgnoreCase(item.encodeMethod)) {
				aeSdecode = EncodeUtil.AESdecodeCBCPKCS7Padding(bytes, keyByte, ivbytes);
			} else {
				try {
					throw new Exception("未知加密方法:" + item.encodeMethod);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
		FileUtil.save(destPath + fnameString, aeSdecode);
		try {
			M38u8downloadMain.done(num);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class Item {
	int num;
	String keyUri;
	String keyStr;
	byte[] keyByte;
	String iv = "0x00000000000000000000000000000000";
	String tsPath;
	boolean useKey = false;
	String encodeMethod;

	public Item(int num, String keyUri, String keyStr, byte[] keyByte, String iv, String tsPath, boolean useKey,
			String encodeMethod) {
		super();
		this.keyStr = keyStr;
		this.num = num;
		this.keyUri = keyUri;
		this.keyByte = keyByte;
		this.iv = iv;
		this.tsPath = tsPath;
		this.useKey = useKey;
		this.encodeMethod = encodeMethod;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getKeyUri() {
		return keyUri;
	}

	public void setKeyUri(String keyUri) {
		this.keyUri = keyUri;
	}

	public String getKeyStr() {
		return keyStr;
	}

	public void setKeyStr(String keyStr) {
		this.keyStr = keyStr;
	}

	public byte[] getKeyByte() {
		return keyByte;
	}

	public void setKeyByte(byte[] keyByte) {
		this.keyByte = keyByte;
	}

	public String getIv() {
		return iv;
	}

	public void setIv(String iv) {
		this.iv = iv;
	}

	public String getTsPath() {
		return tsPath;
	}

	public void setTsPath(String tsPath) {
		this.tsPath = tsPath;
	}

	public boolean isUseKey() {
		return useKey;
	}

	public void setUseKey(boolean useKey) {
		this.useKey = useKey;
	}

	public String getEncodeMethod() {
		return encodeMethod;
	}

	public void setEncodeMethod(String encodeMethod) {
		this.encodeMethod = encodeMethod;
	}

}
