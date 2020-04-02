package com.codesquad.dust;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RestController
public class ApiDustController {

    private Logger logger = LoggerFactory.getLogger(ApiDustController.class);
    private final String SERVEICE_KEY = "sk%2FDXfdYMcYzpqoqa%2FB3wfVChJB7GPbzZgxasEmcfvZogvbX0H77alhv2Ct%2FaXvRw63OsFrEP8MusiURY7xjAQ%3D%3D";

    @GetMapping("")
    public ModelAndView index() {
        return new ModelAndView("index.html");
    }

    @GetMapping("/location")
    public JSONObject stations(@RequestParam(value = "latitude") String latitude,
                               @RequestParam(value = "longitude") String longitude) throws ParseException {
        logger.info("latitude : {}", latitude);
        logger.info("longitude : {}", longitude);
        String dongName = getDongNameFromCoordinate(latitude, longitude);
        List<String> coordinates = getTMCoordinateFromDongName(dongName);
        String stationName = getStationName(coordinates);

        String urlString = "http://openapi.airkorea.or.kr/openapi/services/rest/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty?" +
                "stationName=" + stationName +
                "&dataTerm=daily" +
                "&pageNo=1" +
                "&numOfRows=24" +
                "&ServiceKey=" + SERVEICE_KEY +
                "&ver=1.3" +
                "&_returnType=json";
        String result = requestOpenApi(urlString);
        JSONObject rawDateFromOpenApi = (JSONObject) new JSONParser().parse(result);
        JSONArray secondDate = (JSONArray) rawDateFromOpenApi.get("list");
        JSONArray dustValues = new JSONArray();
        for (int count = 0; count < 24; count++) {
            JSONObject eachData = (JSONObject) secondDate.get(count);
            String pm10Value = (String) eachData.get("pm10Value");
            String pm10Grade = (String) eachData.get("pm10Grade1h");
            String dataTime = (String) eachData.get("dataTime");
            String[] splitDataTime = dataTime.split(" ");
            String[] splitDataTimeTwo = splitDataTime[1].split(":");
            String hour = splitDataTimeTwo[0];
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("pm10Value", pm10Value);
            hashMap.put("pm10Grade", pm10Grade);
            hashMap.put("datetime", hour);

            JSONObject jsonObject = new JSONObject(hashMap);
            dustValues.add(jsonObject);
        }
        JSONObject parsedData = new JSONObject();
        parsedData.put("stationName", stationName);
        parsedData.put("dustValues", dustValues);
        return parsedData;
    }

    private String getStationName(List<String> coordinate) throws ParseException {
        String tmX = coordinate.get(0);
        String tmY = coordinate.get(1);
        String urlString = "http://openapi.airkorea.or.kr/openapi/services/rest/MsrstnInfoInqireSvc/getNearbyMsrstnList?" +
                "tmX=" + tmX +
                "&tmY=" + tmY +
                "&ServiceKey=" + SERVEICE_KEY +
                "&_returnType=json";

        String result = requestOpenApi(urlString);
        JSONObject rawDateFromOpenApi = (JSONObject) new JSONParser().parse(result);
        String stationName = (String) ((JSONObject) ((JSONArray) rawDateFromOpenApi.get("list")).
                get(0)).get("stationName");
        logger.info("stationName : {}", stationName);
        return stationName;
    }

    private List<String> getTMCoordinateFromDongName(String dongName) throws ParseException {

        String urlString = "http://openapi.airkorea.or.kr/openapi/services/rest/MsrstnInfoInqireSvc/getTMStdrCrdnt?" +
                "umdName=" + dongName +
                "&pageNo=1&numOfRows=1" +
                "&ServiceKey=" + SERVEICE_KEY +
                "&_returnType=json";
        String result = requestOpenApi(urlString);
        JSONObject rawDateFromOpenApi = (JSONObject) new JSONParser().parse(result);
        JSONObject parsedData = ((JSONObject) ((JSONArray) rawDateFromOpenApi.get("list")).get(0));
        String tmX = (String) parsedData.get("tmX");
        String tmY = (String) parsedData.get("tmY");
        List<String> coordinates = Arrays.asList(tmX, tmY);
        coordinates.forEach(System.out::println);
        return coordinates;
    }

    private String getDongNameFromCoordinate(String latitude, String longitude) throws ParseException {
        String urlString = "http://api.vworld.kr/req/address?" +
                "service=address&request=getAddress&version=2.0&crs=epsg:4326" +
                "&point=" + longitude + "," + latitude + "&format=json&type=PARCEL&zipcode=true&simple=true" +
                "&key=AAC8C667-87DE-333E-BF82-68EB6EC3A8DC";

        String result = requestOpenApi(urlString);
        JSONObject rawDataFromAPI = (JSONObject) new JSONParser().parse(result);
        logger.info("response : {}", rawDataFromAPI);
        JSONObject response = (JSONObject) rawDataFromAPI.get("response");
        String dongName = (String) (((JSONObject) ((JSONObject) ((JSONArray) response.get("result")).
                get(0)).get("structure"))).get("level4L");
        logger.info("dongName : {}", dongName);
        return dongName;
    }

    private String requestOpenApi(String urlString) {
        StringBuffer result = new StringBuffer();
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));

            String returnLine;
            while ((returnLine = br.readLine()) != null) {
                result.append(returnLine + "\n");
            }
            httpURLConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(result);
    }

    @GetMapping("images")
    public JSONObject images() throws ParseException {
        List<String> imageUrls = Arrays.asList("http://dev-angelo.dlinkddns.com/0401_00.png", "http://dev-angelo.dlinkddns.com/0401_01.png", "http://dev-angelo.dlinkddns.com/0401_02.png", "http://dev-angelo.dlinkddns.com/0401_03.png", "http://dev-angelo.dlinkddns.com/0401_04.png", "http://dev-angelo.dlinkddns.com/0401_05.png", "http://dev-angelo.dlinkddns.com/0401_06.png", "http://dev-angelo.dlinkddns.com/0401_07.png", "http://dev-angelo.dlinkddns.com/0401_08.png", "http://dev-angelo.dlinkddns.com/0401_09.png", "http://dev-angelo.dlinkddns.com/0401_10.png", "http://dev-angelo.dlinkddns.com/0401_11.png", "http://dev-angelo.dlinkddns.com/0401_12.png", "http://dev-angelo.dlinkddns.com/0401_13.png", "http://dev-angelo.dlinkddns.com/0401_14.png", "http://dev-angelo.dlinkddns.com/0401_15.png", "http://dev-angelo.dlinkddns.com/0401_16.png", "http://dev-angelo.dlinkddns.com/0401_17.png", "http://dev-angelo.dlinkddns.com/0401_18.png", "http://dev-angelo.dlinkddns.com/0401_19.png", "http://dev-angelo.dlinkddns.com/0401_20.png", "http://dev-angelo.dlinkddns.com/0401_21.png", "http://dev-angelo.dlinkddns.com/0401_22.png", "http://dev-angelo.dlinkddns.com/0401_23.png");
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(imageUrls);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("images", jsonArray);
        return jsonObject;
    }

    @GetMapping("information")
    public JSONObject information() {
        String informOverall = "수도권·강원영서·충청권·호남권·부산·경남·제주권은 ‘나쁨’, 그 밖의 권역은 ‘보통’으로 예상됨. 다만, 그 밖의 권역에서도 ‘나쁨’ 수준의 농도가 일시적으로 나타날 수 있음";
        String informGrade = "서울 : 나쁨,제주 : 나쁨,전남 : 나쁨,전북 : 나쁨,광주 : 나쁨,경남 : 나쁨,경북 : 보통,울산 : 보통,대구 : 보통,부산 : 나쁨,충남 : 나쁨,충북 : 나쁨,세종 : 나쁨,대전 : 나쁨,영동 : 보통,영서 : 나쁨,경기남부 : 나쁨,경기북부 : 나쁨,인천 : 나쁨";

        JSONObject informaions = new JSONObject();
        informaions.put("informOverall", informOverall);
        informaions.put("informGrade", informGrade);
        return informaions;
    }
}
