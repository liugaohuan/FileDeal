package javalearn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
public class WebCrawlerDemo {
 
    public static void main(String[] args) throws InterruptedException {
        WebCrawlerDemo webCrawlerDemo = new WebCrawlerDemo();
        Map<String, Boolean> oldMap = new LinkedHashMap<String, Boolean>(); // 存储链接-是否被遍历,   键值对
        String baseUrl = "http://pkg.loongnix.cn/loongnix-server/8.4/cloud/loongarch64/release/openstack-ussuri/repodata";
        oldMap.put(baseUrl, false);
        webCrawlerDemo.crawlLinks(baseUrl, oldMap);       
        WebCrawlerDemo.downloadRepo("repo.txt", baseUrl+"/");
        System.out.println("任务完成，已退出");
    }
    /**
     * 抓取所有可以抓取的网页链接，在思路上使用了广度优先算法
     * 对未遍历过的新链接不断发起GET请求，一直到遍历完整个集合都没能发现新的链接
     * 则表示不能发现新的链接了，任务结束
     * 
     * @param baseUrl  
     * @param oldMap  待遍历的链接集合
     * 
     * @return 返回所有抓取到的链接集合
     * */
    private Map<String, Boolean> crawlLinks(String baseUrl, Map<String, Boolean> oldMap) {
        Map<String, Boolean> newMap = new LinkedHashMap<String, Boolean>();
        String oldLink = "";
        for (Map.Entry<String, Boolean> mapping : oldMap.entrySet()) {            
            // 如果没有被遍历过
            if (!mapping.getValue()) {
            	System.out.println("遍历链接:" + mapping.getKey() + "--------check:"+ mapping.getValue());
                oldLink = mapping.getKey();
                System.out.println("oldLink:"+oldLink);
                // 发起GET请求
                try {
                    URL url = new URL(oldLink);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    if (connection.getResponseCode() == 200) {
                        InputStream inputStream = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        String line = "";
                        Pattern pattern = Pattern.compile("<a.*?href=[\"\']?((https?://)?/?[^\"\']+)[\"\']?.*?>(.+)</a>");                       
                        Matcher matcher = null;
                        File file = new File("repodata.txt");
                        FileWriter fw = new FileWriter(file);
                        BufferedWriter bw = new BufferedWriter(fw);
                        while ((line = reader.readLine()) != null) {     
                        	bw.write(line+"\r\n");
                            matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                String tempLink = matcher.group(1).trim(); // 拿到子链接                               
                                if(tempLink.startsWith("?")) 
                                	continue;                             
                                if(tempLink.startsWith("../"))
                                	continue;   
                                if(tempLink.startsWith("http"))
                                	continue;    
//                                System.out.println("tempLink:"+tempLink);
                                bw.write(tempLink+"\r\n");
                                String newLink = mapping.getKey()+"/"+tempLink;     
                                // 判断获取到的链接是否以http开头
                                if (!newLink.startsWith("http")) {
                                    if (newLink.startsWith("/"))
                                        newLink = baseUrl + newLink;                               
                                    else
                                        newLink = baseUrl + "/" + newLink;
                                }
                                String link = null;
								//去除链接末尾的/
                                if(newLink.endsWith("/"))                            
                                  link=newLink.substring(0, newLink.length() - 1);
//                                  System.out.println("check link:"+link);
                                	//去重,并判断是否为空
                                	if (!oldMap.containsKey(link) && !newMap.containsKey(link) && link != null ) {                               
                                		newMap.put(link, false);
                                }                                                        
                            }
                        }
                        bw.close();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
 
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                oldMap.replace(oldLink, false, true);
            }
        }              
        //有新链接，继续遍历
        if (!newMap.isEmpty()) {
        	//递归调用
            oldMap.putAll(crawlLinks(baseUrl, newMap));  //由于Map的特性，不会导致出现重复的键值对           
        }          
        return oldMap;
    }
    
    private static void downloadRepo(String file, String link) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			BufferedReader br2 = null;
			String str = null;
			try {
				while((str=br.readLine()) != null) {
					String[] cmd = new String[] { "/bin/bash", "-c", "wget -v -x -nH "+link+str};
					try {
     				   Process pro = Runtime.getRuntime().exec(cmd);
                     try {
						pro.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                      br2 = new BufferedReader(new InputStreamReader(pro.getErrorStream()));
                     String str2;
                      while((str2 = br2.readLine()) != null) {
                             System.out.println(str2);
     			}}finally {
     				br.close();
     				br2.close();
     			}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
