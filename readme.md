# m3u8-java-download 下载

## 简介  
普通流或AES-128加密均可自动下载解密  

## 已测网站
* 斗鱼视频
* 阿里大学  (注意：该网站m3u8地址只能访问一次，再次访问404)

##  使用方式
* JDK8+  
* 没有做GUI，直接使用源码，填入m3u8网址和输出文件夹，运行main方法即可  
* 默认10线程，可以在源码中调整线程数  
* 如果不能解密，那就要自行更换解密方法  
* 至于 m3u8 地址的查询方法，请自行搜索  

![1.png](1.png)
![2.png](2.png)