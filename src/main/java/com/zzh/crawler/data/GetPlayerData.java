package com.zzh.crawler.data;


import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetPlayerData {
	/**
	 * 全局变量 记录数据条数
	 */
	private static int length = 1;

	/**
	 * httpClient配置 可不用变
	 */
    private PoolingHttpClientConnectionManager cm;
    public GetPlayerData() {
        this.cm = new PoolingHttpClientConnectionManager();
//        设置最大连接数
        this.cm.setMaxTotal(100);
//        设置每个主机最大连接数
        this.cm.setDefaultMaxPerRoute(10);
    }

    public static void main(String[] args) throws Exception {
//        开启爬虫
      new GetPlayerData().getTask();

    }

    public void getTask() throws Exception {
    	//创建excel工作簿
    	WritableWorkbook workbook =  createXlsWorkBook();
    	for (int i = 1; i <= 37; i++) { //这里的循环次数是根据选手的数据页数来定的
    		String url = "https://www.wanplus.com/ajax/player/recent?isAjax=1&playerId=445&gametype=2&page="+i;   //选手id写死了，爬其他选手换id就行
    		//解析地址
    		String json = doGetJson(url);
    		parseJson(workbook,json);
    		System.out.println("第"+i+"页爬取完毕！");
		}
    	//把创建的内容写入到输出流中，并关闭输出流
    	workbook.write();
    	workbook.close();
    	
    }
	/**
	 * 根据请求地址下载页面数据
	 * @param url
	 * @return 页面数据
	 */
	public String doGetJson(String url) {
//    获取httpClient对象
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(this.cm).build();
//     创建httpGet请求对象，设置url地址
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Host","www.wanplus.com");
		httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:70.0) Gecko/20100101 Firefox/70.0");
		httpGet.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
		httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
		httpGet.addHeader("Accept-Encoding","gzip, deflate, br");
		httpGet.addHeader("X-Requested-With","XMLHttpRequest");
		httpGet.addHeader("Connection","keep-alive");
		httpGet.addHeader("Referer","https://www.wanplus.com/lol/player/13781");
		httpGet.addHeader("X-CSRF-Token","101925591");
		httpGet.addHeader("Cookie","wanplus_token=7264960d9e3807fc696f32158f3b724c; wanplus_storage=lf4m67eka3o; wanplus_sid=de1749aea7d8814399075c2d6f4265fe; gameType=2; UM_distinctid=16e581289e5221-0528f45e708f0d-4c302b7a-144000-16e581289e610a; CNZZDATA1275078652=348483589-1573430633-https%253A%252F%252Fwww.baidu.com%252F%7C1574041398; wp_pvid=4111885856; Hm_lvt_f69cb5ec253c6012b2aa449fb925c1c2=1573894882,1573972433,1573974427,1574042447; isShown=1; wanplus_csrf=_csrf_tk_34816727; wp_info=ssid=s9229581258; Hm_lpvt_f69cb5ec253c6012b2aa449fb925c1c2=1574044084");
		httpGet.addHeader("TE","Trailers");
		//        设置请求信息
		httpGet.setConfig(this.getConfig());
//        使用httpClient发起请求,获取响应
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
//            解析响应，返回结果
			if (response.getStatusLine().getStatusCode() == 200) {
//                判断响应体Entity是否不为空,如果不为空，就可以使用Entityutils
				if (response.getEntity() != null) {
					String content = EntityUtils.toString(response.getEntity(), "UTF-8");
//                    System.out.println(content);
					return content;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
//            关闭response
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
//        解析响应，返回结果
		return "";
	}

	/**
	 * 解析json
	 * @param workbook
	 * @param json
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	private void parseJson(WritableWorkbook workbook, String json) throws JsonParseException, JsonMappingException, IOException {
		//利用jackson将json 转化为map
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> matchesMap = (HashMap<String, Object>) mapper.readValue(json,Map.class);
		ArrayList<Map<String, Object>> matchList = (ArrayList<Map<String, Object>>) matchesMap.get("data");
		for (Map<String, Object> matchMap : matchList) {
			String matchDate = handleDate(matchMap.get("matchcreation").toString());
			String heroName = matchMap.get("heroname").toString(); //英雄名字
			String oneSeedName = matchMap.get("oneseedname").toString();//一号种子
			String twoSeedName = matchMap.get("twoseedname").toString();//二号种子
			String vsObj = oneSeedName+" vs "+twoSeedName;
			System.out.println(matchDate+"--"+heroName+"--"+vsObj);
			addDatas(heroName,vsObj,matchDate,workbook,length);
			System.out.println("第"+length+"条爬取完毕！");
			length = length + 1;
		}
	}
    /**
     * 创建excel工作簿
     * @return
     */
    private WritableWorkbook createXlsWorkBook() {
    	WritableWorkbook workbook = null;
    	WritableSheet sheet = null;
    	try {
    		//1.创建工作簿
    		workbook = Workbook.createWorkbook(new File("D:\\clearlove1.xls"));
			//2.创建新的一页
			sheet = workbook.createSheet("clearlove英雄登场次数", 0);
			//3.创建要显示的内容，创建一个单元格，第一个参数是列坐标、第二个参数为行坐标、第三个参数是输入内容
			Label hero = new Label(0,0,"英雄");
			sheet.addCell(hero);
			Label vsObj = new Label(1,0,"战队");
			sheet.addCell(vsObj);
			Label date = new Label(2,0,"时间");
			sheet.addCell(date);
    	} catch (Exception e) {
			e.printStackTrace();
		}
		return workbook;
	}

	/**
	 *将数据添加到excel中
	 * @param heroName
	 * @param vsObj
	 * @param matchDate
	 * @param workbook
	 * @param length 
	 */
	private void addDatas(String heroName, String vsObj, String matchDate, WritableWorkbook workbook, int length) {
		 try {
			WritableSheet sheet = workbook.getSheet("clearlove英雄登场次数");
			Label hero = new Label(0,length,heroName);
			sheet.addCell(hero);
			Label team = new Label(1,length,vsObj);
			sheet.addCell(team);
			Label date = new Label(2,length,matchDate);
			sheet.addCell(date);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	/**
	 * 将2019-08-25 处理成 2019/08/25
	 * @param date
	 * @return
	 */
	private String handleDate(String date) {
		return  date.replace("-", "/");
	}

    //    配置信息 设置请求信息 可不用变
    private RequestConfig getConfig() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(10000)  //数据传输的最长时间
                .build();
        return config;
    }
}

