package com.example.demo.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.checkerframework.checker.units.qual.s;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping(path = "api/v1/product")
public class ProductController {

    private final ProductService productService;

    // above studentService should be autowired aka automatically instantiated for us (aka dependency injections)
    @Autowired
    public ProductController(ProductService productService) {
        // use method inside of StudentService
        this.productService = productService;
    }

    // maybe call the scraper script here?
    // postmapping / putmapping / deletemapping (we can call other methods this.xyz)
    // preferred: productService.addNewProduct(product) vs direct mapping method

    // AVOID!!! lololol - every 15 minutes? (need column to store last updated datetime?)

    // LET'S DO THIS ONE!!!
    // OR, clear everything and only put new stuff in there every get
    // might not need to save, dynamically return from scraper as list of Products
    @GetMapping
    public List<Product> getProducts(@RequestBody HashMap<String, String> queryParams) {
//    public List<Product> getProducts() {

        HashMap<String, HashMap> targetRes;
        HashMap<String, HashMap> walmartRes;
        List<Product> products = new ArrayList<>();

        String searchStr = queryParams.get("searchStr");

        try {
             targetRes = target(searchStr);
        } catch(IOException e) {
            System.out.println(e);
            return new ArrayList<>();
        }

        for (String productName : targetRes.keySet()) {
            HashMap<String, String> productValues = targetRes.get(productName);
            Set productKeySet = targetRes.get(productName).keySet();
            Boolean availability = false;
            if (productKeySet.contains("buyURL") && productKeySet.contains("allStoresAvailability")) {
                if (productValues.get("allStoresAvailability").equals("true")) {
                    availability = true;
                }
                Product newProduct = new Product(productName, productValues.get("brand"), "Target", Double.parseDouble(productValues.get("price")), availability, null, productValues.get("imageURL"), productValues.get("buyURL"));
                products.add(newProduct);
            }
        }

        try {
            walmartRes = walmart(searchStr);
        } catch(IOException e) {
            System.out.println(e);
            return new ArrayList<>();
        }

        for (String productId : walmartRes.keySet()) {
            HashMap<String, String> productValues = walmartRes.get(productId);
//            Set productKeySet = targetRes.get(productId).keySet();
//            Boolean availability = false;
//            if (productKeySet.contains("buyURL") && productKeySet.contains("allStoresAvailability")) {
//                if (productValues.get("allStoresAvailability").equals("true")) {
//                    availability = true;
//                }
            Boolean availability = false;
            if (productValues.get("availability").equals("true")) {
                availability = true;
            }
            Product newProduct = new Product(productValues.get("name"), productValues.get("brand"), "Walmart", Double.parseDouble(productValues.get("price")), availability, null, productValues.get("imageURL"), productValues.get("buyURL"));
            products.add(newProduct);
//            }
        }

        return products;
//        return productService.getProducts();
    }

    @PostMapping
    public void registerNewProduct(@RequestBody Product product) {
        productService.addNewProduct(product);
    }

    @DeleteMapping(path = "{productId}")
    public void deleteProduct(@PathVariable("productId") Long productId) {
        productService.deleteProduct(productId);
    }

    @PutMapping(path = "{productId}")
    public void updateProduct(
            @PathVariable("productId") Long productId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) boolean availability,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String imageURL) {
        productService.updateProduct(productId, name, price, availability, quantity, imageURL);
    }

    public static HashMap<String, HashMap> target(String query_str) throws IOException {

        HashMap<String, HashMap> productsInfo = new HashMap<>();
        ArrayList<String> productsTCINs = new ArrayList<>();

        // TO GET TITLE, BRAND, IMAGEURL, PRICE INFO
//        String query_param = "baby diapers".replace(" ", "%20");
        String query_param = query_str.replace(" ", "+");
        String store_id = "2851";
        String closest_stores = "2851%2C666%2C1236%2C1978%2C1969";
        String zip_code = "43065";
        String latitude = "40.142";
        String longitude = "-83.094";
        String state = "OH";
        String store_name = "Powell".replace(" ", "%20");

        URL url1 = new URL("https://redsky.target.com/redsky_aggregations/v1/web/plp_search_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&channel=WEB&count=24&default_purchasability_filter=true&include_sponsored=true&keyword=" + query_param + "&offset=0&page=%2Fs%2F" + query_param + "&platform=desktop&pricing_store_id=" + store_id + "&scheduled_delivery_store_id=" + store_id + "&store_ids=" + closest_stores + "&useragent=Mozilla%2F5.0+%28Macintosh%3B+Intel+Mac+OS+X+10_15_7%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F103.0.0.0+Safari%2F537.36&visitor_id=01829981AFC20201B704F984195348C8&zip=" + zip_code);
//        URL url1 = new URL("https://redsky.target.com/redsky_aggregations/v1/web/plp_search_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&channel=WEB&count=24&default_purchasability_filter=true&include_sponsored=true&keyword=" + query_param + "&offset=0&page=%2Fs%2F" + query_param + "&platform=desktop&pricing_store_id=" + store_id + "&scheduled_delivery_store_id=" + store_id + "&store_ids=" + closest_stores + "&useragent=Mozilla%2F5.0+%28Macintosh%3B+Intel+Mac+OS+X+10_15_7%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F103.0.0.0+Safari%2F537.36&visitor_id=0182931DD1530201807DA42E454E876C&zip=" + zip_code);
        HttpURLConnection httpConn1 = (HttpURLConnection) url1.openConnection();
        httpConn1.setRequestMethod("GET");

        httpConn1.setRequestProperty("authority", "redsky.target.com");
        httpConn1.setRequestProperty("accept", "application/json");
        httpConn1.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn1.setRequestProperty("cache-control", "no-cache");
        httpConn1.setRequestProperty("cookie", "TealeafAkaSid=EnOjpeW8zWWUeqJVm3Ew6dS3Bh1yWO-E; visitorId=01829981AFC20201B704F984195348C8; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=0c81390c-45b2-4303-8c2d-2a3c57009fff; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyNDFkNjhmOS1mNjAxLTQyODktODkxNS04NzBiMGQ0ODRlNGIiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA1MTkxOTIsImlhdCI6MTY2MDQzMjc5MiwianRpIjoiVEdULjc5ZDM2Nzk3Yzg5ODQwNWJhYWRkN2NiYmY0ZWQ0NjA0LWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6IjFlYWZhNWIxMGE5NWYxZDMyZWFkMTkzMTA3YThlMGQ3ZDE2ZWNlZDE5Zjc0YzQ4NjcyY2Y0YTViMThiYmJmMGUiLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.PUosuuaWzxTxGYwLDGVNvOd1crKNKEJJohIAv-fsHDO9rzdRYDsKgEk_uWPGGIOYrGGgQF58htVzPDuNQqXdkVJFGaf52IxxhbE2opmfjai64zFGysZxvwsE8F5X91ZqO7R1fpee-40f1wSOGbZFE025JoR30T4C0lhUI1r6Ua30KDuVpuHyEI7gtHJ4q1wE7PeyDMLxmOE4AtgEJVjG9sM21ThEodge4eCPZZA0fDrrf5N8DJn756keKJZ2qqJdyy6gOUVMrTp03VWyW0rTmwxubvd4tc2zyisRJClOXtrBtnCk_Vcj_HbR4vOxVykPtiT5NRgo0Vk44hRFrlMJtw; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIyNDFkNjhmOS1mNjAxLTQyODktODkxNS04NzBiMGQ0ODRlNGIiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA1MTkxOTIsImlhdCI6MTY2MDQzMjc5MiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=KfplKcFu66VXvgATXHg7olpbH4neHDDBeaV8SLzCQPDyZcF38PGw1lQS4ZF3_v9Q0KBMi7xXvaj-Pgu2wnbpsg; __gads=ID=53bcdb5e2ec6958d-221518a397d400e5:T=1660432792:S=ALNI_MY2PiPoJPYvQa29hN1JZh85sGSI6Q; __gpi=UID=000007d7eb690e6f:T=1660432792:RT=1660432792:S=ALNI_MZa8I9UWG9DU8x1C6KtVxiQlL-1nA; ffsession={%22sessionHash%22:%22a2ec84a458a2d1660432791938%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _mitata=NmM2NTNlZmMxMjczM2YyM2Q0NTc3OWUwMWMxNjM0MGE4ZGIzNDQwYzgxNTFmNGY1NjhlM2Y2Y2I2YzczNjM3Yw==_/@#/1660432852_/@#/cn87clyI5BsTDLuM_/@#/NTA1Y2ExYmRkOTg3YjE5NTRmYjY1MWQ4ZmY2NGIyYzZjZGNiOWQyOWVmYTczNDkyYmQxMTMxZDkxYTVmODA0Mg==_/@#/000; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; _uetsid=6f8be5a01b5e11ed8c08855033c48b5d; _uetvid=6f8bd3101b5e11ed852b516e08bcb78c; _gcl_au=1.1.2100730937.1660432793");
//        httpConn1.setRequestProperty("cookie", "TealeafAkaSid=XPo04Lyra571aX8zsO4BkxEZJwg1IhEA; visitorId=0182931DD1530201807DA42E454E876C; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=74636cb7-a895-4011-ba7b-b1585c9ecc17; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwianRpIjoiVEdULjJiZTA3M2ExOTRmZDRhNGRiMzMyMWU2MTdiMzBkNjA3LWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6ImQ1M2RlOWQyZWViOWQ2NDQ0N2EzZDRjMmEyNzUyMWVkYjkwMDgxMTRjNmY1NmYxODU5NzQyN2Q0MTQzZTRlNjciLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.vZ15BEok1eHRcjrF4BS5vYtvLtMXQn_4fJk_iOtO4cu3Mq2QQbngTT8GYBmcpMjh4EtnNvhetqfLgX_9aUDbkUaE9M5owfmOIlkSwtbBjtfMO3W23GJjpNaSJc_OUpdmkMsm501YwBcn0X-zWB0IflxrQkKio1WwkfYHiYRTTmiPqlrFMgfRM6wYJAcD0XxwVyLUwF0-HRlVbFOO6_wVLMv_1AJ9jc2IYevo-PW4GrIYd8gMEBn7egFfwovxCz0KzLUTzSZU8GyWySrLPHkFo4A30BvGQEjAd2wTNW2NfPTKYV-7suJaPr6iXeqF7ZJqX7R10GjDixHM6_rdtINDpA; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=CO85uM0VWY02spIC67J3xCDwrJY7rfmTHdm9HkW1NV_GfVjfHm6ILFYDSoG6b9DTFXQfgyk2z7C9dfhv5KytHg; __gads=ID=9368b73264399b1f:T=1660325582:S=ALNI_MbEA7Zhd_F_fSqgKI_mMyTx3UPCKg; __gpi=UID=000007d27b4acfcc:T=1660325582:RT=1660325582:S=ALNI_Maegt2baFi6rDfQs2-rWz40X7V1pw; ffsession={%22sessionHash%22:%22805435c5f35b21660325581765%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _gcl_au=1.1.803936005.1660325583; _mitata=YzA0MGU4Njc2MTE5ZmZlOTk2YmJjZThjYmFkZmRjZWNmMTk1YjFkZDNiYmFkYmVmZDY5Mzc0MDg5OWYxOTE0Nw==_/@#/1660325643_/@#/cgOMrZ7prkr7jS7Z_/@#/OWI4MzdmMmI1ZmZlNDYxYjNiN2Y0ZTc2ZGM0OWIwZGJjNmRiODFiMzc4ZGEwYWVjM2FmY2RmZDE5YmZiZDAzZQ==_/@#/000; _uetsid=d17b95401a6411eda234e96874e82e16; _uetvid=d17bd5401a6411ed89da29522b1058aa; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code);
        httpConn1.setRequestProperty("origin", "https://www.target.com");
        httpConn1.setRequestProperty("pragma", "no-cache");
        httpConn1.setRequestProperty("referer", "https://www.target.com/s?searchTerm=" + query_param);
        httpConn1.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        httpConn1.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn1.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        httpConn1.setRequestProperty("sec-fetch-dest", "empty");
        httpConn1.setRequestProperty("sec-fetch-mode", "cors");
        httpConn1.setRequestProperty("sec-fetch-site", "same-site");
        httpConn1.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

        InputStream responseStream1 = httpConn1.getResponseCode() / 100 == 2
                ? httpConn1.getInputStream()
                : httpConn1.getErrorStream();
        Scanner s1 = new Scanner(responseStream1).useDelimiter("\\A");
        String response1 = s1.hasNext() ? s1.next() : "";

        JsonElement jsonElement1 = new JsonParser().parse(response1);
        Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
        String json1 = gson1.toJson(jsonElement1);

//		System.out.println(json1);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap1 = objectMapper.readValue(json1, new TypeReference<Map<String, Object>>(){});
        Object data1 = jsonMap1.get("data");

        Map<String, Object> dataMap1 = objectMapper.convertValue(data1, Map.class);
        Object search1 = dataMap1.get("search");

        Map<String, Object> searchMap1 = objectMapper.convertValue(search1, Map.class);
        Object products1 = searchMap1.get("products");

        ArrayList<Object> productsAL1 = objectMapper.convertValue(products1, ArrayList.class);
        for (Object product : productsAL1) {
            HashMap<String, String> productInfo = new HashMap<>();

            Map<String, Object> productMap = objectMapper.convertValue(product, Map.class);

            // get title
            Object item = productMap.get("item");
            Map<String, Object> itemMap = objectMapper.convertValue(item, Map.class);
            Object productDescription = itemMap.get("product_description");
            Map<String, Object> productDescriptionMap = objectMapper.convertValue(productDescription, Map.class);
            Object title = productDescriptionMap.get("title");
            String titleStr = objectMapper.convertValue(title, String.class);
            productsInfo.put(titleStr, productInfo);

            // get tcin
            Object productTCIN = productMap.get("tcin");
            String productTCINStr = objectMapper.convertValue(productTCIN, String.class);
            productsTCINs.add(productTCINStr);

            // get brand name
            Object brandInfo = itemMap.get("primary_brand");
            Map<String, Object> brandInfoMap = objectMapper.convertValue(brandInfo, Map.class);
            Object brandName = brandInfoMap.get("name");
            String brandNameStr = objectMapper.convertValue(brandName, String.class);
            productInfo.put("brand", brandNameStr);

            // get price
            Object price = productMap.get("price");
            Map<String, Object> priceMap = objectMapper.convertValue(price, Map.class);
            Object curPrice = priceMap.get("formatted_current_price");
            String curPriceStr = objectMapper.convertValue(curPrice, String.class);
            productInfo.put("price", curPriceStr);

            // get primary image URL
            Object enrichment = itemMap.get("enrichment");
            Map<String, Object> enrichmentMap = objectMapper.convertValue(enrichment, Map.class);
            Object images = enrichmentMap.get("images");
            Map<String, Object> imagesMap = objectMapper.convertValue(images, Map.class);
            Object primaryImage = imagesMap.get("primary_image_url");
            String primaryImageStr = objectMapper.convertValue(primaryImage, String.class);
            productInfo.put("imageURL", primaryImageStr);
        }

        // CONVERT ARRAYLIST OF PRODUCT TCINS INTO STRING
        String productsTCINsStr = String.join("%2C", productsTCINs);

        // TO GET AVAILABILITY INFO AND BUY URL FROM FULFILLMENT DATA
        URL url = new URL("https://redsky.target.com/redsky_aggregations/v1/web_platform/product_summary_with_fulfillment_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&tcins=" + productsTCINsStr + "&store_id"+ store_id +"&zip=" + zip_code + "&state=" + state + "&latitude=" + latitude + "&longitude=" + longitude + "&scheduled_delivery_store_id=" + store_id + "&required_store_id=" + store_id + "&has_required_store_id=true&channel=WEB&page=%2Fs%2F" + query_param);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        httpConn.setRequestProperty("authority", "redsky.target.com");
        httpConn.setRequestProperty("accept", "application/json");
        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn.setRequestProperty("cache-control", "no-cache");
        httpConn.setRequestProperty("cookie", "TealeafAkaSid=EnOjpeW8zWWUeqJVm3Ew6dS3Bh1yWO-E; visitorId=01829981AFC20201B704F984195348C8; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "US; egsSessionId=0c81390c-45b2-4303-8c2d-2a3c57009fff; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyNDFkNjhmOS1mNjAxLTQyODktODkxNS04NzBiMGQ0ODRlNGIiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA1MTkxOTIsImlhdCI6MTY2MDQzMjc5MiwianRpIjoiVEdULjc5ZDM2Nzk3Yzg5ODQwNWJhYWRkN2NiYmY0ZWQ0NjA0LWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6IjFlYWZhNWIxMGE5NWYxZDMyZWFkMTkzMTA3YThlMGQ3ZDE2ZWNlZDE5Zjc0YzQ4NjcyY2Y0YTViMThiYmJmMGUiLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.PUosuuaWzxTxGYwLDGVNvOd1crKNKEJJohIAv-fsHDO9rzdRYDsKgEk_uWPGGIOYrGGgQF58htVzPDuNQqXdkVJFGaf52IxxhbE2opmfjai64zFGysZxvwsE8F5X91ZqO7R1fpee-40f1wSOGbZFE025JoR30T4C0lhUI1r6Ua30KDuVpuHyEI7gtHJ4q1wE7PeyDMLxmOE4AtgEJVjG9sM21ThEodge4eCPZZA0fDrrf5N8DJn756keKJZ2qqJdyy6gOUVMrTp03VWyW0rTmwxubvd4tc2zyisRJClOXtrBtnCk_Vcj_HbR4vOxVykPtiT5NRgo0Vk44hRFrlMJtw; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIyNDFkNjhmOS1mNjAxLTQyODktODkxNS04NzBiMGQ0ODRlNGIiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA1MTkxOTIsImlhdCI6MTY2MDQzMjc5MiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=KfplKcFu66VXvgATXHg7olpbH4neHDDBeaV8SLzCQPDyZcF38PGw1lQS4ZF3_v9Q0KBMi7xXvaj-Pgu2wnbpsg; __gads=ID=53bcdb5e2ec6958d-221518a397d400e5:T=1660432792:S=ALNI_MY2PiPoJPYvQa29hN1JZh85sGSI6Q; __gpi=UID=000007d7eb690e6f:T=1660432792:RT=1660432792:S=ALNI_MZa8I9UWG9DU8x1C6KtVxiQlL-1nA; ffsession={%22sessionHash%22:%22a2ec84a458a2d1660432791938%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _mitata=NmM2NTNlZmMxMjczM2YyM2Q0NTc3OWUwMWMxNjM0MGE4ZGIzNDQwYzgxNTFmNGY1NjhlM2Y2Y2I2YzczNjM3Yw==_/@#/1660432852_/@#/cn87clyI5BsTDLuM_/@#/NTA1Y2ExYmRkOTg3YjE5NTRmYjY1MWQ4ZmY2NGIyYzZjZGNiOWQyOWVmYTczNDkyYmQxMTMxZDkxYTVmODA0Mg==_/@#/000; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; _uetsid=6f8be5a01b5e11ed8c08855033c48b5d; _uetvid=6f8bd3101b5e11ed852b516e08bcb78c; _gcl_au=1.1.2100730937.1660432793");
//        httpConn.setRequestProperty("cookie", "TealeafAkaSid=XPo04Lyra571aX8zsO4BkxEZJwg1IhEA; visitorId=0182931DD1530201807DA42E454E876C; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=74636cb7-a895-4011-ba7b-b1585c9ecc17; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwianRpIjoiVEdULjJiZTA3M2ExOTRmZDRhNGRiMzMyMWU2MTdiMzBkNjA3LWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6ImQ1M2RlOWQyZWViOWQ2NDQ0N2EzZDRjMmEyNzUyMWVkYjkwMDgxMTRjNmY1NmYxODU5NzQyN2Q0MTQzZTRlNjciLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.vZ15BEok1eHRcjrF4BS5vYtvLtMXQn_4fJk_iOtO4cu3Mq2QQbngTT8GYBmcpMjh4EtnNvhetqfLgX_9aUDbkUaE9M5owfmOIlkSwtbBjtfMO3W23GJjpNaSJc_OUpdmkMsm501YwBcn0X-zWB0IflxrQkKio1WwkfYHiYRTTmiPqlrFMgfRM6wYJAcD0XxwVyLUwF0-HRlVbFOO6_wVLMv_1AJ9jc2IYevo-PW4GrIYd8gMEBn7egFfwovxCz0KzLUTzSZU8GyWySrLPHkFo4A30BvGQEjAd2wTNW2NfPTKYV-7suJaPr6iXeqF7ZJqX7R10GjDixHM6_rdtINDpA; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=CO85uM0VWY02spIC67J3xCDwrJY7rfmTHdm9HkW1NV_GfVjfHm6ILFYDSoG6b9DTFXQfgyk2z7C9dfhv5KytHg; __gads=ID=9368b73264399b1f:T=1660325582:S=ALNI_MbEA7Zhd_F_fSqgKI_mMyTx3UPCKg; __gpi=UID=000007d27b4acfcc:T=1660325582:RT=1660325582:S=ALNI_Maegt2baFi6rDfQs2-rWz40X7V1pw; ci_pixmgr=other; _gcl_au=1.1.803936005.1660325583; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; ffsession={%22sessionHash%22:%22805435c5f35b21660325581765%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:4%2C%22prevSearchTerm%22:%22" + query_param + "%22}; _uetsid=d17b95401a6411eda234e96874e82e16; _uetvid=d17bd5401a6411ed89da29522b1058aa; _mitata=NzhlZGE0NTQzNjU0NTU4ZWRhYjdmMzI1MGQ1ZDgwNzRmNzc5N2NiOTY2YmNlYzMzNjE2MWNhZWQwNmY0ZDA0YQ==_/@#/1660327519_/@#/cgOMrZ7prkr7jS7Z_/@#/ZmE0Njg0MzQ2NDVjNWM3MWMxMDdlNDI2ZmQxZmJhNDk5OTU5M2FlMzllMzMyYjkwYTc1MTNiZTMzMzMyODk4Mg==_/@#/000");
        httpConn.setRequestProperty("origin", "https://www.target.com");
        httpConn.setRequestProperty("pragma", "no-cache");
        httpConn.setRequestProperty("referer", "https://www.target.com/s?searchTerm=" + query_param);
        httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        httpConn.setRequestProperty("sec-fetch-dest", "empty");
        httpConn.setRequestProperty("sec-fetch-mode", "cors");
        httpConn.setRequestProperty("sec-fetch-site", "same-site");
        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        JsonElement jsonElement = new JsonParser().parse(response);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(jsonElement);

        System.out.println("LOOK AT THIS");
//        System.out.println(json);

        Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        Object data = jsonMap.get("data");

        Map<String, ArrayList<Object>> dataMap = objectMapper.convertValue(data, Map.class);
        ArrayList<Object> productSummaries = dataMap.get("product_summaries");

        for (Object product : productSummaries) {
            Map<String, Object> productMap = objectMapper.convertValue(product, Map.class);


            Object productItem = productMap.get("item");
            Map<String, Object> productItemMap = objectMapper.convertValue(productItem, Map.class);


            // get title
            Object productDescription = productItemMap.get("product_description");
            Map<String, Object> productDescriptionMap = objectMapper.convertValue(productDescription, Map.class);
            Object productTitle = productDescriptionMap.get("title");
//			System.out.println(productTitle);
            String productTitleStr = objectMapper.convertValue(productTitle, String.class);
            HashMap<String, String> selectedProduct = null;
            if (productsInfo.get(productTitleStr) != null) {
                selectedProduct = productsInfo.get(productTitleStr);
            } else {
                System.out.println("NOT FOUND! CHECK FOR ERRORS!");
                System.out.println(productTitleStr);
                continue;
            }

            // get brand

            // get purchase url
            Object enrichment = productItemMap.get("enrichment");
            Map<String, String> enrichmentMap = objectMapper.convertValue(enrichment, Map.class);
            String buyURL = enrichmentMap.get("buy_url");
            selectedProduct.put("buyURL", buyURL);

            // get image url

            // get price
            Object productPrice = productMap.get("price");
            Map<String, Double> productPriceMap = objectMapper.convertValue(productPrice, Map.class);
            Double productRTP = productPriceMap.get("current_retail");
            selectedProduct.put("price", productRTP.toString());

            // get availability
            Object productFulfillment = productMap.get("fulfillment");
//			System.out.println(productFulfillment);
            Map<String, Object> productFulfillmenteMap = objectMapper.convertValue(productFulfillment, Map.class);
            // all store locations
            boolean productInStock = objectMapper.convertValue(productFulfillmenteMap.get("is_out_of_stock_in_all_store_locations"), boolean.class);
            String inStockAtAllStores = objectMapper.convertValue(!(productInStock), String.class);
            selectedProduct.put("allStoresAvailability", inStockAtAllStores);
//			// primary store location
//			Object storeOptions = productFulfillmenteMap.get("store_options");
//			ArrayList<Object> storeOptionsAL = objectMapper.convertValue(storeOptions, ArrayList.class);
//			Object chosenStoreOption = storeOptionsAL.get(0);
//			Map<String, Object> chosenStoreOptionMap = objectMapper.convertValue(chosenStoreOption, Map.class);
//			Object inStoreOnly = chosenStoreOptionMap.get("in_store_only");
//			Map<String, String> inStoreOnlyMap = objectMapper.convertValue(inStoreOnly, Map.class);
//			String inStoreOnlyStatus = inStoreOnlyMap.get("availability_status");
//			Object orderPickup = chosenStoreOptionMap.get("order_pickup");
//			Map<String, String> orderPickupMap = objectMapper.convertValue(orderPickup, Map.class);
//			String orderPickupStatus = orderPickupMap.get("availability_status");
//			if (inStoreOnlyStatus.equals("IN_STOCK") || orderPickupStatus.equals("IN_STOCK")) {
//				selectedProduct.put("closestStoreAvailability", "true");
//			} else {
//				selectedProduct.put("closestStoreAvailability", "false");
//			}
//			System.out.println("-----------------------------------------------------------------------");
        }

        return productsInfo;
//        Gson gsonFinal = new GsonBuilder().setPrettyPrinting().create();
//        String jsonFinal = gson.toJson(productsInfo);
//        System.out.println(jsonFinal);
    }

    public static HashMap<String, HashMap> walmart(String query_str) throws IOException {
        HashMap<String, HashMap> products = new HashMap<>();

//        String query_param = "vitamin c".replace(" ", "%20");
        String query_param = query_str.replace(" ", "%20");
        // System.out.println(String.format("https://www.walmart.com/orchestra/home/graphql/search?query=%s&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=%s&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=false&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS", query_param, query_param));
        URL url = new URL(String.format("https://www.walmart.com/orchestra/home/graphql/search?query=%s&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=%s&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=false&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS", query_param, query_param));
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("authority", "www.walmart.com");
        httpConn.setRequestProperty("accept", "application/json");
        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn.setRequestProperty("cache-control", "no-cache");
        httpConn.setRequestProperty("content-type", "application/json");
        httpConn.setRequestProperty("cookie", "QuantumMetricUserID=1b74567bde43517f2d6f05ce031d4174; brwsr=cfd042ac-b896-11ec-95b6-41dad55d245b; vtc=ZQEKmh8wbDUTmajKGEX264; _pxvid=f230629f-de29-11ec-8e01-43685866486c; _abck=ijx6zhigxlc4pcaigas8_1982; wm_ul_plus=INACTIVE|1653878769217; WMP=4; oneapp_customer=true; _gcl_au=1.1.899230111.1654187236; s_pers_2=om_mv3d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1654450057037%3B%20%2Bom_mv7d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1654795635601%3B%20%2Bom_mv14d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1655400435601%3B%20%2Bom_mv30d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1656782835601%3B%20useVTC%3DY%7C1717302429%3B%20om_mv7d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1654795657038%3B%20om_mv14d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1655400457039%3B%20om_mv30d%3Daff%3Aadid-%3Asourceid-imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80%3Awmls-imp_1924488%3Acn-%7C1656782857039; _m=5; _mc=w4gsWbTg0xkrfQgD9HILwVZ6v6K8N1MWZbgGt5GB7oE%3D; _ga=GA1.2.273003026.1655925375; _pxhd=48ce569874e7cd889801984565bb87e5201c493f26cb184dcf817845eb30c404:f230629f-de29-11ec-8e01-43685866486c; TS013ed49a=01538efd7cfca0d42335465517f5b7f323b776b83819246b3706f4ec8b40384b383974f11ebf767fea70ae590dec3bb7ff9575d049; pxcts=ec36027a-02b7-11ed-bc9e-7551764d517a; _sp_id.ad94=36e22216-6f8e-483e-9ee9-09cf91242034.1653852715.7.1657908281.1657905359.e2622c52-32cd-415a-9ee2-962ecdf69ca3; akavpau_p1=1657909918~id=14b887157d1ed615d2de81397a1f72a9; TBV=7; _vc=2OJpNSzuUzCmgF6gTlPAMbJOZodHe5UVPLQ%2Bf43VUAo%3D; rtoken=MDgyNTUyMDE4MPD5wLV63wE6dW1TLhB%2F8wPtScoKmdCW8EQmEbLCBG41lqRL19UULAO5vLMfZRsrtxfcRLRZPa426GII7ltTeGWcHBeJeOrRi%2BkfY3gUvC7BAwYZMKT2hMdkxRIbqc%2Bcp35krirml1mhC%2FJ7HDpiUcfpUSt2NnSxmBrsvS7kZEJ3FNKJalZNsIvLoY%2B7rfrJot8OeBqCLaPydL1XFj8DtSt%2FCfaYAXvteOZzeGUvSRnM3I4JEklk6eAXW4GMBJ77tCXzTLjXqEC6dkiikM6kWEF6Pj%2Byj7rPXBD5Z%2BCsM7GYJKHFzn0XwpwpeufCfGob6OcrmFydbBOCpJADN4cwuwJZeRUYSpqTKoY%2FD8tESaUYqJtlQF6DxOhMnYwfMBUKL%2BS%2Fczl4Kqb1ns%2FsRFhN0Y5Je%2BWo0WP%2FZxmCBgXHUeI%3D; SPID=88e22adcc196bbba262702c9c789aa42da041976be7aa6d6f09a9986002b32130103080d40eddc19efbde44d88129db0cprof; CID=a3179fde-3160-4911-8c21-918967d2e9ec; hasCID=1; customer=%7B%22firstName%22%3A%22Yingyue%22%2C%22lastNameInitial%22%3A%22G%22%7D; type=REGISTERED; userContext=eyJhZGRyZXNzRGF0YSI6eyJoYXNEZWxpdmVyYWJsZUFkZHJlc3MiOnRydWV9LCJoYXNJdGVtU3Vic2NyaXB0aW9uIjpmYWxzZSwiaGFzTWVtYmVyc2hpcEluZm8iOnRydWUsImlzRGVmYXVsdCI6ZmFsc2UsInBheW1lbnREYXRhIjp7ImNhcGl0YWxPbmVCYW5uZXJTbm9vemVUUyI6MCwiaGFzQ2FwT25lIjpmYWxzZSwiaGFzQ2FwT25lTGlua2VkIjpmYWxzZSwiaGFzQ3JlZGl0Q2FyZCI6dHJ1ZSwiaGFzRGlyZWN0ZWRTcGVuZENhcmQiOmZhbHNlLCJoYXNFQlQiOmZhbHNlLCJoYXNHaWZ0Q2FyZCI6dHJ1ZSwic2hvd0NhcE9uZUJhbm5lciI6dHJ1ZSwid3BsdXNOb0JlbmVmaXRCYW5uZXIiOnRydWV9LCJwcm9maWxlRGF0YSI6eyJpc0Fzc29jaWF0ZSI6ZmFsc2UsImlzVGVzdEFjY291bnQiOmZhbHNlLCJtZW1iZXJzaGlwT3B0SW4iOnsiaXNPcHRlZEluIjpmYWxzZSwib3B0ZWRJbkF0dHJpYnV0ZUlkIjoiODZiNTViYTctNDM5NS00ZmJkLTlkNDQtODU5NDY5N2RkYWNmIn19fQ%3D%3D; AID=wmlspartner%253D0%253Areflectorid%253D0000000000000000000000%253Alastupd%253D1658956066948; _uetvid=6c4168608db611ec9379317f76753255; tb_sw_supported=true; DL=98105%2C%2C%2Cip%2C98105%2C%2C; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImlzRXhwbGljaXQiOmZhbHNlLCJpbnRlbnQiOiJTSElQUElORyIsInBpY2t1cCI6W3siYnVJZCI6IjAiLCJub2RlSWQiOiIzMDk4IiwiZGlzcGxheU5hbWUiOiJCZWxsZXZ1ZSBOZWlnaGJvcmhvb2QgTWFya2V0Iiwibm9kZVR5cGUiOiJTVE9SRSIsImFkZHJlc3MiOnsicG9zdGFsQ29kZSI6Ijk4MDA3IiwiYWRkcmVzc0xpbmUxIjoiMTUwNjMgTWFpbiBTdCIsImNpdHkiOiJCZWxsZXZ1ZSIsInN0YXRlIjoiV0EiLCJjb3VudHJ5IjoiVVMiLCJwb3N0YWxDb2RlOSI6Ijk4MDA3LTUyMjUifSwiZ2VvUG9pbnQiOnsibGF0aXR1ZGUiOjQ3LjYwOTAzNiwibG9uZ2l0dWRlIjotMTIyLjEzOTQ4N30sImlzR2xhc3NFbmFibGVkIjp0cnVlLCJzY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJ1blNjaGVkdWxlZEVuYWJsZWQiOmZhbHNlLCJodWJOb2RlSWQiOiIzMDk4Iiwic3RvcmVIcnMiOiIwNjowMC0yMjowMCIsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbIlBJQ0tVUF9DVVJCU0lERSIsIlBJQ0tVUF9JTlNUT1JFIl19XSwic2hpcHBpbmdBZGRyZXNzIjp7ImlkIjoiZTdhZjY1MjQtNDIwNy00ZTE4LWJmOGItNjdhZTBkZGY4OWU3IiwiYWRkcmVzc0xpbmVPbmUiOiI0NzMwIFVuaXZlcnNpdHkgV2F5IE5FIiwiYWRkcmVzc0xpbmVUd28iOiJTdGUgMjA2IiwibGF0aXR1ZGUiOjQ3LjY2MzUzMywibG9uZ2l0dWRlIjotMTIyLjMxMjg5MSwicG9zdGFsQ29kZSI6Ijk4MTA1NDQyNCIsImNpdHkiOiJTZWF0dGxlIiwic3RhdGUiOiJXQSIsImNvdW50cnlDb2RlIjoiVVNBIiwiaXNBcG9GcG8iOmZhbHNlLCJpc1BvQm94IjpmYWxzZSwiYWRkcmVzc1R5cGUiOiJSRVNJREVOVElBTCIsImxvY2F0aW9uQWNjdXJhY3kiOiJoaWdoIiwibW9kaWZpZWREYXRlIjoxNjQ0OTg0NDQzMDIzLCJnaWZ0QWRkcmVzcyI6ZmFsc2UsImZpcnN0TmFtZSI6Illpbmd5dWUiLCJsYXN0TmFtZSI6IkdvaW5ncyJ9LCJhc3NvcnRtZW50Ijp7Im5vZGVJZCI6IjMwOTgiLCJkaXNwbGF5TmFtZSI6IkJlbGxldnVlIE5laWdoYm9yaG9vZCBNYXJrZXQiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImluc3RvcmUiOmZhbHNlLCJyZWZyZXNoQXQiOjE2NjAwMTI2MzQ5MzMsInZhbGlkYXRlS2V5IjoicHJvZDp2MjphMzE3OWZkZS0zMTYwLTQ5MTEtOGMyMS05MTg5NjdkMmU5ZWMifQ%3D%3D; assortmentStoreId=3098; hasLocData=1; TB_Latency_Tracker_100=1; TB_Navigation_Preload_01=1; TB_SFOU-100=; bstc=SIagoB2tyG62i7hTVu8vng; mobileweb=0; xpa=1A0pE|4eEiX|5mXkC|78jM7|927zv|9aQs1|AIud-|Af1yH|CN28l|E4WND|IOIpg|LTD5Y|MZ8aF|Nnski|NuISU|O1c3v|QFWT5|RZlxg|SmVSa|TA_mk|X6RCs|XBPw9|ZKQTc|bcl64|cfVAR|dK8qH|duBe9|elin2|iaKHs|kUFvx|lqVt_|n3tYK|nybjr|oDpYF|oIQlM|odFJG|phpX6|q826r|qsDvB|qyn67|qzcBg|r8csb|rxmwe|uJQh6|uru_L|yefCT|yxNJ6|zCylr|zIYIr; exp-ck=4eEiX178jM71927zv19aQs12Af1yH1E4WND1MZ8aF1Nnski1NuISU1O1c3v2QFWT51SmVSa1TA_mk2X6RCs1XBPw91dK8qH1iaKHs1kUFvx1n3tYK1nybjr2oIQlM1odFJG1phpX61q826r1qsDvB1qyn672r8csb1yefCT1yxNJ65zIYIr1; _astc=a90c415f4aa683a6946d96c09b6578e6; xpm=1%2B1659991034%2BZQEKmh8wbDUTmajKGEX264~a3179fde-3160-4911-8c21-918967d2e9ec%2B0; dimensionData=743; wmlh=d4831d3c66d3a76c555ec3826b29dac4556990c5a6188d6c05b7f34ff1192735; bm_mi=1E9824AAD8CBBE1D0B66C94539231D9D~YAAQFY0duDSiiGOCAQAAVQUwfxDuC6u0QHSlyyBoxf4sb6HpBqAJ7ZHei31AdPzgLKlNTSZ3wOzNkPH+J1qK1GcgoMLsrGOcGaZnGmuaBC/3VznEhZHk5yqOuY+BYpiXZ5g4O/PmLa3TWw/bxFzCkb00/TXui1THxEWbx9Jr6/GF0fU3lD0yV+NnMZQCvv7WvFjJhooOzspBegVubMWN5s7gnia8a3lNK3914TCQDRfkdyB7Q/LEn3Sb4yPcI76sA7EFpQN9EENlHAhyPDNfOioCOJmVr8vZtDz67wyMP4pvP9MM6/25mJt2Q6IGq5C58WHurjQ=~1; bm_sv=FF0C069C5A54A66787A1AD1A59970A2B~YAAQFY0duE+riGOCAQAA7UAwfxDR+MQ+SqkCkvobaDx7HLV4VQH5NzYwoCWJwgwndFlwil3mNaQA4YdHHIa/qxLncnZMqJ63DegILmdWNxrVnDtIs6VjVY8osWmu3B0J1QiguljA6ZjJUXc+U/MvWpm+cX0b+7+bpbQcHMJzK6BBfoOzLufjDd+tp9GyEVZ9dt3p5UQVbR4QiWIDbKuFxQFF2hf49wOwtqJu8MdQN5RM8y0kxGc2/YI4g+pZcmd15ac=~1; ak_bmsc=E9B2A792FB3C45F5FF3256E5AFFF5305~000000000000000000000000000000~YAAQFY0duJariGOCAQAAk0IwfxDI8W5ztmzNG0SG0pXdhuUfRSntCY8tj5Y4ozd2G+Jk8sfy/f+UTEy63P5PeBc9kGxTVt1TDuCXkim8YlQnA1tPPyEpfX9GItifYvQ6Wjnlp7VEe2vldV2pq3bmG8w9r9R/DvBtEu5TTmfg2FJjhi94IlXjxRTJKLSHhAF8+vfvmLHrpHv0vd9X3S0c5qxNHJHPBkBJp+bWYXdsl1Oq77D8l3glq7Bg6Qdq4YH814Mmz0yCUSUmgqFM9Oz7f3usXfNX11DJnNS0qsa1x3LUTD0ezkt1tdSMDxPgwpI6QCVtH1jvuNns/DplVvDq7Q/mDiwNnH+enadgEVWcDm3YdR16B9ZksHcLgV8UUJa/iFErhgtf+dAoTSJrEqFemvdk4zy0K1kVC1/Gdeuev6tMQ+4AHJcIqOKfOuu8n2a/L/PL0ys=; adblocked=true; com.wm.reflector=\"reflectorid:imp_QNXzy4XWkxyIRLjXHcR4YXWRUkDx7ZQiESQYU80@lastupd:1659991637000@firstcreate:1654187256686\"; TS01b0be75=01538efd7cb0f7d70d3f2a4f0184e2dfb62522b821df71739c28bee1098661c999cf79df51e8c1fe3f71e480b215f28df2770b502e; auth=MTAyOTYyMDE4%2FlD8kJIulEW7LgRHWgcH94ei7v0GYSi3oby5oozhfRD4ZzF9B0xi8xy1%2FVQ%2Fs3Dm7u1n4XQ7wE7VSsKevYOuY9fGXWdEY%2F24RTdMylPqkvhvmJGVGaqwo%2BHDx6bZQptJEk8ozGbo5MEn1vkTvRAX7b7LUiIDuTvf5uU3uvySDLvFZsa13wS3MTN7U8eO5wXn43u2Fuo%2B2u39VG2IRjjn2TWK8DRV7WegEDnNeeLB8fE%2FRn9yGf%2FIUg7988ZIy%2FAl9gq5HzyXSnOoFi9bPAYpjSsQ5HsQluZiyTdisdCvQao5iZ1YH1p%2FztKBuD3RTboo%2B19Gsh6kCyN%2FRZN%2BRDk5D6sxB2psTVmBueMQLCOsWd7HhfaU2QlRCS1TvguTy8BWLf%2F5yvR5dY1HivPS2AYXZpacwyh4jmkr9TcCIXD4RZk%3D; xptwg=3312775449:156C9E1226C3610:376E951:85554D8D:2323FFBB:27BC18AB:; TS012768cf=0162aeaf8603e1df16ad86ebb5a0adf83b53d26253fcd943fbcdd82662836da53a4095af02264ed456939d0bc1976b84d95a078d09; TS01a90220=0162aeaf8603e1df16ad86ebb5a0adf83b53d26253fcd943fbcdd82662836da53a4095af02264ed456939d0bc1976b84d95a078d09; TS2a5e0c5c027=08281e41a8ab2000b3b2223b213663b430113057e560ae289edfb58b0568623b971c67aa967f3e490820678f7a1130006b222de4e06a27846604bedcf0e5269837a80419599d340389e78e1e431b30fbb10712d171509f6d78902cbb7f30d071; akavpau_p2=1659992571~id=bbde09c6234375969238c0fca4213b0f");
        httpConn.setRequestProperty("device_profile_ref_id", "ypgq4T9i5Oi1JN7brkk0rgeejZx0rrHpAIhq");
        httpConn.setRequestProperty("origin", "https://www.walmart.com");
        httpConn.setRequestProperty("pragma", "no-cache");
        httpConn.setRequestProperty("referer", String.format("https://www.walmart.com/search?q=%s", query_param));
        httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        httpConn.setRequestProperty("sec-fetch-dest", "empty");
        httpConn.setRequestProperty("sec-fetch-mode", "cors");
        httpConn.setRequestProperty("sec-fetch-site", "same-origin");
        httpConn.setRequestProperty("traceparent", "DRmPfZEvmPQ42cVmI3XUawgFu8VdvXfR3I8E");
        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        httpConn.setRequestProperty("wm_mp", "true");
        httpConn.setRequestProperty("wm_page_url", String.format("https://www.walmart.com/search?q=%s", query_param));
        httpConn.setRequestProperty("wm_qos.correlation_id", "DRmPfZEvmPQ42cVmI3XUawgFu8VdvXfR3I8E");
        httpConn.setRequestProperty("x-apollo-operation-name", "Search");
        httpConn.setRequestProperty("x-enable-server-timing", "1");
        httpConn.setRequestProperty("x-latency-trace", "1");
        httpConn.setRequestProperty("x-o-bu", "WALMART-US");
        httpConn.setRequestProperty("x-o-ccm", "server");
        httpConn.setRequestProperty("x-o-correlation-id", "DRmPfZEvmPQ42cVmI3XUawgFu8VdvXfR3I8E");
        httpConn.setRequestProperty("x-o-gql-query", "query Search");
        httpConn.setRequestProperty("x-o-mart", "B2C");
        httpConn.setRequestProperty("x-o-platform", "rweb");
        httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-da9c05");
        httpConn.setRequestProperty("x-o-segment", "oaoh");

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write(String.format("{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"%s\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"%s\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"%s\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"%s\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"enableFacetCount\":false,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\",\"pageType\":\"SearchPage\"}}", query_param, query_param, query_param, query_param));
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        JsonElement jsonElement = new JsonParser().parse(response);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(jsonElement);

        System.out.println(json);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        Object data = jsonMap.get("data");
        Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);

        // GETTING BRANDS

        Object contentLayout = dataMap.get("contentLayout");
        Map<String, Object> contentLayoutMap = objectMapper.convertValue(contentLayout, Map.class);

        Object modules = contentLayoutMap.get("modules");
        ArrayList<Object> modulesAL = objectMapper.convertValue(modules, ArrayList.class);

        System.out.println(String.format("LENGTH OF MODULES LIST: %d", modulesAL.size()));

        Object chosenModule = modulesAL.get(1);
        Map<String, Object> moduleMap = objectMapper.convertValue(chosenModule, Map.class);
        Object type = moduleMap.get("type");
        String typeStr = objectMapper.convertValue(type, String.class);
        if (!typeStr.equals("SearchSortFilterModule")) {
            chosenModule = modulesAL.get(2);
            moduleMap = objectMapper.convertValue(chosenModule, Map.class);
        }

        Object configs = moduleMap.get("configs");
        Map<String, Object> configsMap = objectMapper.convertValue(configs, Map.class);

        Object topNavFacets = configsMap.get("topNavFacets");
        ArrayList<Object> topNavFacetsAL = objectMapper.convertValue(topNavFacets, ArrayList.class);

        Object chosenFacet = topNavFacetsAL.get(2);
        Map<String, Object> chosenFacetMap = objectMapper.convertValue(chosenFacet, Map.class);

        Object values = chosenFacetMap.get("values");
        ArrayList<Object> brandsAL = objectMapper.convertValue(values, ArrayList.class);

        HashMap<String, String> brands = new HashMap<String, String>();

        for (Object brand : brandsAL) {
            Map<String, String> brandInfo = objectMapper.convertValue(brand, Map.class);
            brands.put(brandInfo.get("name").toLowerCase(), brandInfo.get("name"));
        }

        System.out.println("BRANDS!!!");
        System.out.println(brands);

        // GETTING PRODUCTS

        Object search = dataMap.get("search");

        Map<String, Object> searchMap = objectMapper.convertValue(search, Map.class);
        Object searchResult = searchMap.get("searchResult");

        Map<String, Object> searchResultMap = objectMapper.convertValue(searchResult, Map.class);
        Object itemStacks = searchResultMap.get("itemStacks");

        ArrayList<Object> itemStacksAL = objectMapper.convertValue(itemStacks, ArrayList.class);
        Object firstInAL = itemStacksAL.get(0);

        Map<String, Object> itemsV2Map = objectMapper.convertValue(firstInAL, Map.class);
        Object itemsV2 = itemsV2Map.get("itemsV2");

        ArrayList<Object> itemsAL = objectMapper.convertValue(itemsV2, ArrayList.class);

        for (Object o : itemsAL) {

            HashMap<String,String> newProduct = new HashMap<>();

            Map<String, Object> item = objectMapper.convertValue(o, Map.class);

            // get id
            Object productID = item.get("usItemId");
            String productIDStr = objectMapper.convertValue(productID, String.class);
//			System.out.println("productID: " + productIDStr);
            products.put(productIDStr, newProduct);

            // get name
            Object name = item.get("name");
            String nameStr = objectMapper.convertValue(name, String.class).toLowerCase();
//			System.out.println("name: " + name);
            newProduct.put("name", nameStr);

            // get buyURL
            Object buyURL = item.get("canonicalUrl");
            String buyURLStr = objectMapper.convertValue(buyURL, String.class);
            int toIndex = buyURLStr.indexOf("?");
            if (toIndex <= buyURLStr.length() && toIndex >= 0) {
                buyURLStr = buyURLStr.substring(0, toIndex);
            }
//			System.out.println("buyURL: " + buyURLStr);
            newProduct.put("buyURL", buyURLStr);

            //get brand
            boolean foundBrand = false;

            for (String brand : brands.keySet()) {
                if (nameStr.contains(brand)) {
                    foundBrand = true;
//					System.out.println("brand: " + brands.get(brand));
                    newProduct.put("brand", brands.get(brand));
                    break;
                }
            }

            if (!foundBrand) {
                for (String brand : brands.keySet()) {
                    if (isBrand(nameStr, brand)) {
//						System.out.println("brand: " + brands.get(brand));
                        newProduct.put("brand", brands.get(brand));
                        break;
                    }
                }
            }

            // get availability
            Map<String, Object> itemAvailability = objectMapper.convertValue(item.get("availabilityStatusV2"), Map.class);
//			System.out.println("availability: " + itemAvailability.get("display"));
            Object inStockStatus = itemAvailability.get("display");
            String inStockStatusStr = objectMapper.convertValue(inStockStatus, String.class);
            String availabilityStr = "false";
            if (inStockStatusStr.equals("In stock")) {
                availabilityStr = "true";
            }
            newProduct.put("availability", availabilityStr);

            // get price
            Map<String, Object> itemPriceInfo = objectMapper.convertValue(item.get("priceInfo"), Map.class);
            if (itemPriceInfo.get("currentPrice") != null) {
                Map<String, Integer> itemCurPrice = objectMapper.convertValue(itemPriceInfo.get("currentPrice"), Map.class);
                Object price = itemCurPrice.get("price");
                String priceStr = objectMapper.convertValue(price, String.class);
//				System.out.println("price: " + itemCurPrice.get("price"));
                newProduct.put("price", priceStr);
            } else if (itemPriceInfo.get("priceRange") != null) {
                Map<String, Double> itemPriceRange = objectMapper.convertValue(itemPriceInfo.get("priceRange"), Map.class);
                Double minPrice = itemPriceRange.get("minPrice");
                Double maxPrice = itemPriceRange.get("maxPrice");
                if (minPrice != maxPrice) {
//					System.out.println("min price: " + minPrice);
//					System.out.println("max price: " + maxPrice);
                    String maxPriceStr = objectMapper.convertValue(maxPrice, String.class);
                    newProduct.put("price", maxPriceStr);
                } else {
//					System.out.println("price: " + minPrice);
                    String maxPriceStr = objectMapper.convertValue(maxPrice, String.class);
                    newProduct.put("price", maxPriceStr);
                }

            }

            // get thumbnail image URL
            Object imageInfo = item.get("imageInfo");
            Map<String, Object> imageInfoMap = objectMapper.convertValue(imageInfo, Map.class);
            Object thumbnailURL = imageInfoMap.get("thumbnailUrl");
            String thumbnailURLStr = objectMapper.convertValue(thumbnailURL, String.class);
            newProduct.put("imageURL", thumbnailURLStr);
//			System.out.println("imageURL: " + thumbnailURL);
//			System.out.println("--------------------------------------------------------");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // GET INDIVIDUAL PICKUP VS DELIVERY VS SHIPPING AVAILABILITY OPTIONS
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        return products;


//		URL url = new URL("https://www.walmart.com/orchestra/home/graphql/ip");
//		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//		httpConn.setRequestMethod("POST");
//
//		httpConn.setRequestProperty("authority", "www.walmart.com");
//		httpConn.setRequestProperty("accept", "application/json");
//		httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
//		httpConn.setRequestProperty("cache-control", "no-cache");
//		httpConn.setRequestProperty("calltype", "CLIENT");
//		httpConn.setRequestProperty("content-type", "application/json");
//		httpConn.setRequestProperty("cookie", "AID=wmlspartner%253D0%253Areflectorid%253D0000000000000000000000%253Alastupd%253D1660428100121; auth=MTAyOTYyMDE4ehDWX%2FpHyXfIOYK3T6MTIDUvBJyRsIx6ZRQ1yJdfPhmawXPN6qIU8XXHXolMyaFuJberVZ4dOTPPgNyt1mRpPG9W%2BBnWb1UMwns8Ex5E4l4rQLT6HHHYf%2BrPchUOkR14767wuZloTfhm7Wk2Kcjygi5k0VvBM%2FJjwcKWWhCnBS%2FeCA9hHbWyiKxs58%2BAL16ICOVoaNl7QhAB9kTzFMcXb0Kg8eKKIDEu%2F8QfVzHKQSoUMk70P8glgOEpLOprhDfMM%2FFHGZ2dCNmxWrdkwqEKrqA%2Bl%2B2vk7qcJK5QqjevkIX9E0GlJd9qt7xfYAwcJ2OgxH1h8DZxbdDqR7kW%2FDV%2B8smjmcFosupf5PhyibK2fKRUIQPCAduHVVxzh9AJFlZBc8Ff2%2BT%2Bngoug3mdNkRb%2F5E5WBBdZBCyKnCQAR7o6eg%3D; ACID=979aacd3-ce1e-4b0b-8a3c-d7286e2ba30b; hasACID=true; assortmentStoreId=" + storeID + "; hasLocData=1; TB_Latency_Tracker_100=1; TB_Navigation_Preload_01=1; TB_SFOU-100=; vtc=R_xfpnBXvr_njANSx6Yv1o; bstc=R_xfpnBXvr_njANSx6Yv1o; mobileweb=0; xpa=2SWkj|5q86Y|9l3gx|AQhLM|Af1yH|CN28l|GkqrP|HDfyl|IOIpg|LTD5Y|MRnMk|PiOGS|PzQ_l|SmVSa|T-onc|TA_mk|W7hWw|XBPw9|XU_XK|bcl64|ccDGr|elin2|fZHdi|jncu9|lqVt_|n3tYK|nzyw-|oDpYF|qyn67|r8csb|s9Rs7|uJQh6|uru_L|vwMwa|yefCT|yxNJ6|zCylr; exp-ck=2SWkj1AQhLM1Af1yH1HDfyl2MRnMk1PzQ_l1SmVSa1TA_mk2W7hWw2XBPw91XU_XK1ccDGr1fZHdi1jncu91n3tYK1nzyw-1qyn672r8csb1yefCT1yxNJ65; _pxhd=c35188f9383b21b96ae1b7f36df86a14261c16b319c0af68aa34fb66c1c1881d:824e2ed8-1b53-11ed-8a3b-6d6465557345; pxcts=8341d6af-1b53-11ed-9c85-77416e716e54; _pxvid=824e2ed8-1b53-11ed-8a3b-6d6465557345; TBV=7; adblocked=false; dimensionData=821; xpm=1%2B1660428100%2BR_xfpnBXvr_njANSx6Yv1o~%2B0; ak_bmsc=028C8FE51D66E78C527BFC1268D68E46~000000000000000000000000000000~YAAQhS4gF7vr2pCCAQAASiw6mRCxc80NIAGBUCnB45zpMxQ4fnc+zG3xVl/tkEP5mMps8hQB1/ck/swj28q+cSS3dTqbasXZWwDWZkqmQDnmYz9fZnwBnTx/71Ih0aun7NCfY4YuXTbXhDcHc256as9leKTD+MqGqiQq9BaUzmesxn/sywoY8mkliYWYcsz7qK5UHSH4Vo+x2A9G1e4nqdzmLstSzkSMAe4j5lj66NYcEQ+sYJjDchEbVLIh19AAYbZYln/Ft5UUC7Agw686PzoNtYeml3PvqaDp7FkI73I4gsqcSSkMgk9NrV6ghpreasTa6ebn+Szcyjc31sbJAJdQFUDecggyTnMUWbr71zPGtcmfLKKUQnXQAt13v0Okya5NpJH23PGOg+Lx0o7gAq65lGcf2CCiigIhlyrisi0sUvJzeqHkGzNkfjp1KjOCpidcEi2KxEMDH7/eDh42k65bbhl9Iq4DR+3KQFYxBGFbsm5l5oj7LoJk5pmZ; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImlzRXhwbGljaXQiOmZhbHNlLCJpbnRlbnQiOiJTSElQUElORyIsInBpY2t1cCI6W3siYnVJZCI6IjAiLCJub2RlSWQiOiIzMDk4IiwiZGlzcGxheU5hbWUiOiJCZWxsZXZ1ZSBOZWlnaGJvcmhvb2QgTWFya2V0Iiwibm9kZVR5cGUiOiJTVE9SRSIsImFkZHJlc3MiOnsicG9zdGFsQ29kZSI6Ijk4MDA3IiwiYWRkcmVzc0xpbmUxIjoiMTUwNjMgTWFpbiBTdCIsImNpdHkiOiJCZWxsZXZ1ZSIsInN0YXRlIjoiV0EiLCJjb3VudHJ5IjoiVVMiLCJwb3N0YWxDb2RlOSI6Ijk4MDA3LTUyMjUifSwiZ2VvUG9pbnQiOnsibGF0aXR1ZGUiOjQ3LjYwOTAzNiwibG9uZ2l0dWRlIjotMTIyLjEzOTQ4N30sImlzR2xhc3NFbmFibGVkIjp0cnVlLCJzY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJ1blNjaGVkdWxlZEVuYWJsZWQiOmZhbHNlLCJodWJOb2RlSWQiOiIzMDk4Iiwic3RvcmVIcnMiOiIwNjowMC0yMjowMCIsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbIlBJQ0tVUF9DVVJCU0lERSIsIlBJQ0tVUF9JTlNUT1JFIl19XSwic2hpcHBpbmdBZGRyZXNzIjp7ImxhdGl0dWRlIjo0Ny42Mjk3LCJsb25naXR1ZGUiOi0xMjIuMzQ1NSwicG9zdGFsQ29kZSI6Ijk4MTA5IiwiY2l0eSI6IlNlYXR0bGUiLCJzdGF0ZSI6IldBIiwiY291bnRyeUNvZGUiOiJVU0EiLCJnaWZ0QWRkcmVzcyI6ZmFsc2V9LCJhc3NvcnRtZW50Ijp7Im5vZGVJZCI6IjMwOTgiLCJkaXNwbGF5TmFtZSI6IkJlbGxldnVlIE5laWdoYm9yaG9vZCBNYXJrZXQiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImluc3RvcmUiOmZhbHNlLCJyZWZyZXNoQXQiOjE2NjA0NDk3MDI3OTMsInZhbGlkYXRlS2V5IjoicHJvZDp2Mjo5NzlhYWNkMy1jZTFlLTRiMGItOGEzYy1kNzI4NmUyYmEzMGIifQ%3D%3D; locGuestData=eyJpbnRlbnQiOiJTSElQUElORyIsImlzRXhwbGljaXQiOmZhbHNlLCJzdG9yZUludGVudCI6IlBJQ0tVUCIsIm1lcmdlRmxhZyI6ZmFsc2UsImlzRGVmYXVsdGVkIjpmYWxzZSwicGlja3VwIjp7Im5vZGVJZCI6IjMwOTgiLCJ0aW1lc3RhbXAiOjE2NjA0MjgxMDI3ODJ9LCJwb3N0YWxDb2RlIjp7InRpbWVzdGFtcCI6MTY2MDQyODEwMjc4MiwiYmFzZSI6Ijk4MTA5In0sInZhbGlkYXRlS2V5IjoicHJvZDp2Mjo5NzlhYWNkMy1jZTFlLTRiMGItOGEzYy1kNzI4NmUyYmEzMGIifQ%3D%3D; _astc=17106784aec13ceb9f7f56e853d3e22b; wmlh=605f07329326543945783f506b7bc67e2d7edb360aa0a19de005602f816d9f84; tb_sw_supported=true; _px3=458bfc2183896896eb2e5691613a5bc47397f2afba584d54d5a4009aba243593:oYUm6Yst+aWs0jeJbx7q95qjLvq1ylOYCakGNm99j2na2eYWyw5dI/PKTbXO3JuLypx7DWMKuFVnAytO5WwPeA==:1000:l39eR+lg9KPLwI9KotshZTp8OQBGRBny+pkwz9/+bir1Dq8alK+QkIp9Aslu15T2PWCS2mRnz6DW2WIYlvy40hrjMtKrqYnPRG6FwzAMSXlbJncK9clxYWhPJ/f2F7yB1VCjWug1MYHxmK55ymDwD2tyuTKmeLmtg9dXKyCo45VeXvvzUmDjiy0ce58IcfoqFjhRLMcw0MU3nRqRAGTesw==; akavpau_p2=1660429506~id=a9d389f31620e93dec1fc86c31a45525; com.wm.reflector=\"reflectorid:0000000000000000000000@lastupd:1660428907000@firstcreate:1660428100121\"; xptwg=234446872:23DEBDCBF6FBF00:5CC8A47:AFEC17BD:92B4E032:92A4B3BF:; TS012768cf=01bf728746d87b920173c3106c389fdcdc83c01d00963e0c114a166bd04b52e890282d9fb91fec7187f2c46036a0505fd6d4bcbf24; TS01a90220=01bf728746d87b920173c3106c389fdcdc83c01d00963e0c114a166bd04b52e890282d9fb91fec7187f2c46036a0505fd6d4bcbf24; TS2a5e0c5c027=08096cbb3cab200093650148a7e5bf599e7cab1f432ed2c06ac2757cec17ef649090efae25d6237b08d4a1bd62113000ace2cba864045cb807ef2f7cccf54f4d8143695d456e967a3c0d3204add8d66a76ae2c54747f4df87290a9939219011e; bm_sv=0363A9DCDC1CEFA0EF37C07A079CEA49~YAAQHHZiaOOEdZeCAQAAA3hGmRANfJe22Ci4MI9XtxvzuhQOhWpk9c/CNU+EAiKKAWd7iRSOxYo0iS4U9ISwqjAKMZ1Kx9ri8yP86HZSEaz/lFaBp4O8Pe8TVbA8P58SfbF13GHDSSejYahsT6kN5FZNfTbdDvNje5ecTjLNojQxJAhQocLK4QtUUKaMW3u4WcggmZCjEARvogZinySK6nHwbVPX8lnhutEQpgJ55uB/2y9a0kIRQ83zffb1UjvKsL4=~1");
//		httpConn.setRequestProperty("device_profile_ref_id", "AlN3Xyk-Y_qK1cDMtkr2Bmfwk7IH0CsVaGIx");
//		httpConn.setRequestProperty("is-variant-fetch", "false");
//		httpConn.setRequestProperty("origin", "https://www.walmart.com");
//		httpConn.setRequestProperty("pragma", "no-cache");
//
//		httpConn.setRequestProperty("referer", "https://www.walmart.com" + buyURL);
////		httpConn.setRequestProperty("referer", "https://www.walmart.com/ip/Degree-Men-Original-Antiperspirant-Deodorant-Cool-Rush-Mens-Deodorant-Stick-48-Hour-Odor-Protection-2-7-oz-2-Count/10898692?athbdg=L1200");
//		httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
//		httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//		httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
//		httpConn.setRequestProperty("sec-fetch-dest", "empty");
//		httpConn.setRequestProperty("sec-fetch-mode", "cors");
//		httpConn.setRequestProperty("sec-fetch-site", "same-origin");
//		httpConn.setRequestProperty("traceparent", "3vSAQDBYCxa9HWfRmIq3909HNhivFyiMpTe-");
//		httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
//		httpConn.setRequestProperty("wm_mp", "true");
//		httpConn.setRequestProperty("wm_page_url", "https://www.walmart.com" + buyURL);
//		httpConn.setRequestProperty("wm_qos.correlation_id", "3vSAQDBYCxa9HWfRmIq3909HNhivFyiMpTe-");
//		httpConn.setRequestProperty("x-apollo-operation-name", "ItemById");
//		httpConn.setRequestProperty("x-enable-server-timing", "1");
//		httpConn.setRequestProperty("x-latency-trace", "1");
//		httpConn.setRequestProperty("x-o-bu", "WALMART-US");
//		httpConn.setRequestProperty("x-o-ccm", "server");
//		httpConn.setRequestProperty("x-o-correlation-id", "3vSAQDBYCxa9HWfRmIq3909HNhivFyiMpTe-");
//		httpConn.setRequestProperty("x-o-gql-query", "query ItemById");
//		httpConn.setRequestProperty("x-o-item-id", itemID);
//		httpConn.setRequestProperty("x-o-mart", "B2C");
//		httpConn.setRequestProperty("x-o-platform", "rweb");
//		httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
//		httpConn.setRequestProperty("x-o-segment", "oaoh");
//
//		httpConn.setDoOutput(true);
//		OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//		writer.write("{\"query\":\"query ItemById( $iId:String! $vFId:String $pAdd:PostalAddress $sFId:[StoreFrontId]$p13N:P13NRequest $p13nCls:JSON $layout:[String]$tempo:JSON $pageType:String! $vTOP:JSON $sSId:Int $cSId:String $fBB:Boolean! $fBBAd:Boolean! $fMq:Boolean! $fSL:Boolean! $fSCar:Boolean! $fFit:Boolean! $fIdml:Boolean! $fRev:Boolean! $fulInt:String $fSeo:Boolean! $fP13:Boolean! $fAff:Boolean! $fDis:Boolean! $eItIb:Boolean! $fIlc:Boolean! ){contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:\\\"WM_GLASS\\\" version:\\\"v1\\\" ){modules(p13n:$p13nCls tempo:$tempo virtualTryOnParams:$vTOP){configs{__typename...on EnricherModuleConfigsV1 @include(if:$fP13){zoneV1}...on TempoWM_GLASSWWWItemCompleteTheLookConfigsV1{title subTitle products{...CompleteTheLookProduct}p13nCtlColumn1{...CompleteTheLookColumn}p13nCtlColumn2{...CompleteTheLookColumn}p13nCtlColumn3{...CompleteTheLookColumn}}...on TempoWM_GLASSWWWBlitzConfigs{onlineBlitzMessageText1 onlineBlitzMessageText2 onlineBlitzMessageText3 inStoreBlitzMessageText1 inStoreBlitzMessageText2 inStoreBlitzMessageText3 pulses{pulseMessageLine1 pulseMessageLine2}}...on TempoWM_GLASSWWWItemCarouselConfigsV1 @include(if:$fP13){products{...ContentLayoutProduct}subTitle title type spBeaconInfo{adUuid moduleInfo pageViewUUID placement max}}...on TempoWM_GLASSWWWItemFitmentModuleConfigs @include(if:$fFit){fitment{partTypeID partTypeIDs result{status notes position formId quantityTitle resultSubTitle suggestions{id position loadIndex speedRating searchQueryParam labels{...FitmentLabel}fitmentSuggestionParams{id value}cat_id}extendedAttributes{...FitmentFieldFragment}labels{...FitmentLabel}}labels{...FitmentLabel}savedVehicle{...FitmentVehicleFragment}}}...on TempoWM_GLASSWWWItemRelatedShelvesConfigs @include(if:$fSeo){seoItemRelmData(id:$iId){relm{id url name}}}...on TempoWM_GLASSWWWWalmartPlusBannerConfigsV1{heading disclaimer ctaLink{linkText title clickThrough{type value}}isDownloadBanner}...on TempoWM_GLASSWWWWalmartPlusEarlyAccessBeforeEventConfigsV1{earlyAccessLogo{alt src}earlyAccessCardMesssage earlyAccessLink1{linkText clickThrough{value}}earlyAccessLink2{linkText clickThrough{value}}}...on TempoWM_GLASSWWWWalmartPlusEarlyAccessDuringEventConfigsV1{earlyAccessLogo{alt src}earlyAccessCardMesssage earlyAccessSubText earlyAccessLink1{linkText clickThrough{value}}}...on TempoWM_GLASSWWWProductWarrantyPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWGeneralWarningsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductIndicationsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductDescriptionPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWRefurbishedInfoPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductDirectionsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductSpecificationsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWNutritionValuePlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWReviewsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWComparisonChartsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWStyliticsModulePlaceholderConfigs{_rawConfigs}...on TempoWM_GLASSWWWAppDownloadPOVConfigs{heading ctaLink{linkText clickThrough{type value}}}...on TempoWM_GLASSWWWMarketingContentBtfConfigs{heading expandedOnPageLoad}...on TempoWM_GLASSWWWProductDescriptionPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWZeekitConfigs{zeekit{...Zeekit}}...CapitalOneBannerFragment...BrandBoxDisplayAdConfigsFragment @include(if:$fBB)...BuyBoxAdConfigsFragment @include(if:$fBBAd)...MarqueeDisplayAdConfigsFragment @include(if:$fMq)...SkylineDisplayAdConfigsFragment @include(if:$fSL)...SponsoredProductCarouselConfigsFragment @include(if:$fSCar)}moduleId matchedTrigger{pageType pageId zone inheritable}name type version status publishedDate}layouts(layout:$layout){id layout}pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessType accessPointId spokeNodeId}pageContext p13nMetadata lazyModules}}seoItemMetaData(id:$iId){noIndex}product( catalogSellerId:$cSId itemId:$iId postalAddress:$pAdd storeFrontIds:$sFId selected:true semStoreId:$sSId p13N:$p13N variantFieldId:$vFId fulfillmentIntent:$fulInt ){...FullProductFragment}idml(itemId:$iId html:true) @include(if:$fIdml){...IDMLFragment}reviews(itemId:$iId) @include(if:$fRev){...FullReviewsFragment}}fragment CapitalOneBannerFragment on TempoWM_GLASSWWWCapitalOneBannerConfigsV1{bannerBackgroundColor primaryImage{alt src}bannerCta{ctaLink{linkText title clickThrough{value}uid}textColor}bannerText{text isBold isUnderlined underlinedColor textColor}}fragment FullProductFragment on Product{blitzItem giftingEligibility shipAsIs subscription{showSubscriptionModule subscriptionEligible subscriptionTransactable}subscriptionEligible showSubscriptionModule discounts @include(if:$fDis){discountedValue{priceString}discountMetaData{type savings{percent}unitPrice{priceString}}}rewards @include(if:$eItIb){eligible state minQuantity rewardAmt promotionId selectionToken cbOffer description term expiry}showFulfillmentLink additionalOfferCount legalRestriction shippingRestriction availabilityStatus averageRating suppressReviews brand brandUrl badges{...BadgesFragment}rhPath aaiaBrandId manufacturerProductId productTypeId tireSize tireLoadIndex tireSpeedRating viscosity model buyNowEligible earlyAccessEvent isEarlyAccessItem isWplusMember showBuyWithWplus preOrder{...PreorderFragment}ozarkAttributes{shippingPromise exactAddress}canonicalUrl catalogSellerId sellerReviewCount sellerAverageRating category{...ProductCategoryFragment}classType classId fulfillmentTitle shortDescription fulfillmentType fulfillmentBadge checkStoreAvailabilityATC checkAvailabilityGlobalDFS fulfillmentLabel{checkStoreAvailability wPlusFulfillmentText message shippingText fulfillmentText locationText fulfillmentMethod addressEligibility fulfillmentType postalCode}fulfillmentOptions @include(if:$fIlc){...on ShippingOptionV2{__typename type selected intent availableQuantity maxOrderQuantity orderLimit speedDetails{fulfillmentBadge deliveryDate fulfillmentPrice{price priceString}freeFulfillment wPlusEligible}locationText availabilityStatus inventoryStatus{fulfillmentBadge quantity}restricted}...on PickupOptionV2{__typename type selected intent availableQuantity maxOrderQuantity orderLimit speedDetails{fulfillmentBadge deliveryDate fulfillmentPrice{price priceString}freeFulfillment wPlusEligible}locationText availabilityStatus inventoryStatus{fulfillmentBadge quantity}productLocation{displayValue}addressLine checkStoreAvailability}...on DeliveryOptionV2{__typename type selected intent availableQuantity maxOrderQuantity orderLimit speedDetails{fulfillmentBadge deliveryDate fulfillmentPrice{price priceString}freeFulfillment wPlusEligible}locationText availabilityStatus}...on DigitalOptionV2{__typename type}}hasSellerBadge hasCarePlans hasHomeServices itemType id primaryUsItemId conditionType imageInfo{...ProductImageInfoFragment}location{postalCode stateOrProvinceCode city storeIds addressId pickupLocation{storeId accessPointId accessType}}manufacturerName name personalizable externalInfo{url}numberOfReviews orderMinLimit orderLimit weightIncrement offerId offerType priceInfo{priceDisplayCodes{...PriceDisplayCodesFragment}currentPrice{...ProductPriceFragment priceDisplay}wasPrice{...ProductPriceFragment}comparisonPrice{priceString}unitPrice{...ProductPriceFragment}savings{priceString}savingsAmount{priceString}subscriptionPrice{price priceString intervalFrequency duration percentageRate subscriptionString}priceRange{minPrice maxPrice priceString currencyUnit denominations{price priceString selected}}listPrice{...ProductPriceFragment}capType walmartFundedAmount{...ProductPriceFragment}}returnPolicy{returnable freeReturns returnWindow{value unitType}returnPolicyText}fsaEligibleInd sellerId sellerName sellerDisplayName secondaryOfferPrice{currentPrice{priceType priceString price}}semStoreData{pickupStoreId deliveryStoreId isSemLocationDifferent}shippingOption{...ShippingOptionFragment}type pickupOption{slaTier accessTypes availabilityStatus storeName storeId}salesUnit usItemId variantCriteria{id categoryTypeAllValues isFitPredictable isSizeChartApplicable name type variantList{availabilityStatus id images name products swatchImageUrl selected}}variants{...MinimalProductFragment}groupMetaData{groupComponents{quantity offerId componentType productDisplayName}}upc wfsEnabled sellerType ironbankCategory snapEligible promoData @include(if:$fAff){id description terms type templateData{priceString imageUrl aprString}noInterestInstallmentsPromotion{bank installments payments currencyAmount currencyUnit}}showAddOnServices addOnServices{serviceType serviceTitle serviceSubTitle groups{groupType groupTitle assetUrl shortDescription unavailabilityReason services{name displayName offerId usItemId selectedDisplayName serviceMetaData currentPrice{price priceString}giftEligible}}}productLocation{displayValue}zeekitData{...Zeekit}experienceType}fragment Zeekit on ZeekitEntity{color abstractProductId zeekit{numberOfSimulatedImages isProductEnabledInZeekit simulatedImages{id url zoomable}personaDetails{id size height firstName bust waist hips inseam}i18n{labels{name value}}}}fragment BadgesFragment on UnifiedBadge{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn}}tags{__typename...on BaseBadge{id text key}}}fragment ShippingOptionFragment on ShippingOption{availabilityStatus slaTier deliveryDate maxDeliveryDate shipMethod shipPrice{...ProductPriceFragment}}fragment ProductCategoryFragment on ProductCategory{categoryPathId path{name url}}fragment PreorderFragment on PreOrder{isPreOrder}fragment MinimalProductFragment on Variant{availabilityStatus discounts{discountedValue{priceString}}subscriptionEligible imageInfo{...ProductImageInfoFragment}priceInfo{priceDisplayCodes{...PriceDisplayCodesFragment}currentPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}}productUrl usItemId id:productId fulfillmentBadge}fragment ProductImageInfoFragment on ProductImageInfo{allImages{id url zoomable}thumbnailUrl}fragment PriceDisplayCodesFragment on PriceDisplayCodes{clearance eligibleForAssociateDiscount finalCostByWeight priceDisplayCondition reducedPrice rollback submapType}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString currencyUnit}fragment NutrientFragment on Nutrient{name amount dvp childNutrients{name amount dvp}}fragment NutritionAttributeFragment on NutritionAttribute{name mainNutrient{...NutrientFragment}childNutrients{...NutrientFragment childNutrients{...NutrientFragment}}}fragment IdmlAttributeFragment on IdmlAttribute{name value attribute}fragment ServingAttributeFragment on ServingAttribute{name values{...IdmlAttributeFragment values{...IdmlAttributeFragment}}}fragment IDMLFragment on Idml{sizeCharts{id tagDisplay url}chokingHazards{...LegalContentFragment}directions{name value}indications{name value}ingredients{activeIngredientName{name value}activeIngredients{name value}inactiveIngredients{name value}ingredients{name value}}longDescription shortDescription interactiveProductVideo specifications{name value}warnings{name value}warranty{information length}esrbRating mpaaRating nutritionFacts{calorieInfo{...NutritionAttributeFragment}keyNutrients{name values{...NutritionAttributeFragment}}vitaminMinerals{...NutritionAttributeFragment}servingInfo{...ServingAttributeFragment}additionalDisclaimer{...IdmlAttributeFragment values{...IdmlAttributeFragment values{...IdmlAttributeFragment}}}staticContent{...IdmlAttributeFragment values{...IdmlAttributeFragment values{...IdmlAttributeFragment}}}}videos{poster title versions{small large}}product360ImageContainer{name url}marketingDescription{name value}}fragment FullReviewsFragment on ProductReviews{averageOverallRating aspects{id name score snippetCount}lookupId customerReviews{...CustomerReviewsFragment}ratingValueFiveCount ratingValueFourCount ratingValueOneCount ratingValueThreeCount ratingValueTwoCount roundedAverageOverallRating topNegativeReview{rating reviewSubmissionTime userNickname negativeFeedback positiveFeedback reviewText reviewTitle badges{badgeType id contentType glassBadge{id text}}syndicationSource{logoImageUrl contentLink name}}topPositiveReview{rating reviewSubmissionTime userNickname negativeFeedback positiveFeedback reviewText reviewTitle badges{badgeType id contentType glassBadge{id text}}syndicationSource{logoImageUrl contentLink name}}totalReviewCount}fragment LegalContentFragment on LegalContent{ageRestriction headline headline image mature message}fragment CustomerReviewsFragment on CustomerReview{rating reviewSubmissionTime reviewText reviewTitle userNickname photos{caption id sizes{normal{...ReviewPhotoSizeFragment}thumbnail{...ReviewPhotoSizeFragment}}}badges{badgeType id contentType glassBadge{id text}}syndicationSource{logoImageUrl contentLink name}}fragment FitmentLabel on FitmentLabels{links{...FitmentLabelEntity}messages{...FitmentLabelEntity}ctas{...FitmentLabelEntity}images{...FitmentLabelEntity}}fragment FitmentVehicleFragment on FitmentVehicle{vehicleType{...FitmentVehicleFieldFragment}vehicleYear{...FitmentVehicleFieldFragment}vehicleMake{...FitmentVehicleFieldFragment}vehicleModel{...FitmentVehicleFieldFragment}additionalAttributes{...FitmentVehicleFieldFragment}}fragment FitmentVehicleFieldFragment on FitmentVehicleField{id value label}fragment FitmentFieldFragment on FitmentField{id value displayName data{value label}extended dependsOn}fragment FitmentLabelEntity on FitmentLabelEntity{id label}fragment ReviewPhotoSizeFragment on ReviewPhotoSize{id url}fragment CompleteTheLookColumn on TempoWM_GLASSWWWP13nCtlColumn{isAnchor cellSpan index}fragment CompleteTheLookProduct on Product{name brand catalogSellerId canonicalUrl availabilityStatus externalInfo{url}id imageInfo{thumbnailUrl}priceInfo{currentPrice{...ProductPriceFragment}}usItemId}fragment ContentLayoutProduct on Product{name badges{...BadgesFragment}canonicalUrl classType availabilityStatus showAtc personalizable externalInfo{url}averageRating fulfillmentBadge fulfillmentSpeed fulfillmentTitle fulfillmentType itemType groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}imageInfo{thumbnailUrl}numberOfReviews offerId orderMinLimit orderLimit weightIncrement p13nDataV1{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}previouslyPurchased{label}preOrder{...PreorderFragment}priceInfo{currentPrice{...ProductPriceFragment priceDisplay}listPrice{...ProductPriceFragment}subscriptionPrice{priceString}priceDisplayCodes{clearance eligibleForAssociateDiscount finalCostByWeight priceDisplayCondition reducedPrice rollback submapType}priceRange{minPrice maxPrice priceString}unitPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}savingsAmount{priceString}capType walmartFundedAmount{...ProductPriceFragment}}rhPath salesUnit sellerId sellerName hasSellerBadge seller{name sellerId}shippingOption{slaTier shipMethod}showOptions snapEligible sponsoredProduct{spQs clickBeacon spTags}usItemId variantCount variantCriteria{name id variantList{name swatchImageUrl selectedProduct{usItemId canonicalUrl}}}}fragment BrandBoxDisplayAdConfigsFragment on TempoWM_GLASSWWWBrandBoxDisplayAdConfigs{_rawConfigs}fragment BuyBoxAdConfigsFragment on TempoWM_GLASSWWWBuyBoxAdConfigs{_rawConfigs moduleLocation lazy ad{...SponsoredProductsAdFragment}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SponsoredProductCarouselConfigsFragment on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs moduleType ad{...SponsoredProductsAdFragment}}fragment SponsoredProductsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredProductsFragment}}}fragment AdDataSponsoredProductsFragment on AdData{...on SponsoredProducts{adUuid adExpInfo moduleInfo products{...ProductFragment}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}\",\"variables\":{\"channel\":\"WWW\",\"pageType\":\"ItemPageGlobal\",\"tenant\":\"WM_GLASS\",\"version\":\"v1\",\"iId\":\"" + itemID + "\",\"layout\":[\"itemDesktop\"],\"tempo\":{\"targeting\":\"%7B%22userState%22%3A%22loggedIn%22%7D\",\"params\":[{\"key\":\"expoVars\",\"value\":\"expoVariationValue\"},{\"key\":\"expoVars\",\"value\":\"expoVariationValue2\"}]},\"p13N\":{\"userClientInfo\":{\"isZipLocated\":true,\"deviceType\":\"desktop\"},\"userReqInfo\":{\"refererContext\":{\"source\":\"itempage\",\"catId\":\"\",\"facet\":\"\",\"query\":\"deodorant\"},\"pageUrl\":\"" + buyURL + "?athbdg=L1200\"}},\"p13nCls\":{\"pageId\":\"" + itemID + "\",\"userClientInfo\":{\"isZipLocated\":true},\"userReqInfo\":{\"refererContext\":{\"source\":\"itempage\"}},\"p13NCallType\":\"ATF\"},\"fBBAd\":true,\"fSL\":true,\"fIdml\":true,\"fRev\":true,\"fFit\":true,\"fSeo\":true,\"fP13\":true,\"fAff\":true,\"fMq\":true,\"fSCar\":true,\"fBB\":true,\"fDis\":false,\"eItIb\":true,\"fIlc\":false}}");
//		writer.flush();
//		writer.close();
//		httpConn.getOutputStream().close();
//
//		InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//				? httpConn.getInputStream()
//				: httpConn.getErrorStream();
//		Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//		String response = s.hasNext() ? s.next() : "";
//		JsonElement jsonElement = new JsonParser().parse(response);
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
//		String json = gson.toJson(jsonElement);
//		System.out.println(json);
    }

    public static boolean isBrand(String name, String brand) {
        String[] words = name.split(" ");
        for (String word : words) {
            if (word.length() > 1 && (oneLetterAway(word, brand) || oneLetterAway(brand, word))) {
                return true;
            }
        }
        return false;
    }

    public static boolean oneLetterAway(String word1, String word2) {
        if (word1.equals(word2.substring(1))) {
            return true;
        } else if (word1.equals(word2.substring(0, word2.length() - 1))) {
            return true;
        } else {
            for (int i = 1; i < word2.length() - 1; i++) {
                if (word1.contains(word2.substring(0, i) + word2.substring(i + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

}
