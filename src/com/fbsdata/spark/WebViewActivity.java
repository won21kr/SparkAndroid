package com.fbsdata.spark;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fbsdata.spark.api.SparkClient;
import com.fbsdata.spark.api.SparkSession;
import com.flexmls.flexmls_api.ApiParameter;
import com.flexmls.flexmls_api.Configuration;
import com.flexmls.flexmls_api.Connection;
import com.flexmls.flexmls_api.ConnectionApacheHttp;
import com.flexmls.flexmls_api.FlexmlsApiClientException;
import com.flexmls.flexmls_api.Response;

public class WebViewActivity extends Activity {
	
	// class vars *************************************************************
	
	private static final String TAG = "WebViewActivity";
    public static final String EXTRA_URL = "com.fbsdata.spark.webview.URL";

    // instance vars **********************************************************
    
    private SparkClient sparkClient;
    
    // interface **************************************************************
    
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_view);
		
		Intent intent = getIntent();
		// will need to load which type of login
		//String url = intent.getStringExtra(EXTRA_URL);		
		
		WebView webView = (WebView) findViewById(R.id.webview);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webView.setWebViewClient(new SparkWebViewClient());
		
	    Configuration c = new Configuration();
	    c.setApiKey(SparkClient.sparkClientKey);
	    c.setEndpoint(SparkClient.sparkAPIEndpoint);
	    c.setSsl(true);
	    this.sparkClient = new SparkClient(c);
	    String loginURL = sparkClient.getSparkHybridOpenIdURLString();
		webView.loadUrl(loginURL);
	    
		//webView.loadUrl(SparkClient.sparkOpenIdLogoutURL);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_web_view, menu);
		return true;
	}
	
	private class SparkWebViewClient extends WebViewClient
	{
		/*
		public void onPageFinished (WebView view, String url)
		{
			if(url.equals(SparkClient.sparkOpenIdLogoutURL))
			{
			    Configuration c = new Configuration();
			    c.setApiKey("cx8re5r5jqh5w2uqbxg7aymb8");
			    SparkClient sparkAPI = new SparkClient(c);
			    String loginURL = sparkAPI.getSparkHybridOpenIdURLString();
				Log.d(TAG, "loginUrl>" + loginURL);
			    view.loadUrl(loginURL);
			}
		}
		*/
		public boolean shouldOverrideUrlLoading (WebView view, String url)
		{
			Log.d(TAG, "loadUrl>" + url);
			
			Uri uri = Uri.parse(url);
			String openIdMode = null;
			String openIdSparkCode = null;
			
		    if((openIdMode = uri.getQueryParameter("openid.mode")) != null &&
		       openIdMode.equals("id_res") &&
		       (openIdSparkCode = uri.getQueryParameter("openid.spark.code")) != null)
		       {
				   Log.d(TAG, "openIdSparkCode>" + openIdSparkCode);
				   
				   new OAuth2PostTask().execute(openIdSparkCode);
				   				   
		    	   return true;
		       }
		    else
		    {
		    	
		    }

			return false;
		}
	}
	
	 private class OAuth2PostTask extends AsyncTask<String, Void, SparkSession> {
	     protected SparkSession doInBackground(String... openIdSparkCode) {
			   Map<String,String> map = new HashMap<String,String>();
			   map.put("client_id", SparkClient.sparkClientKey);
			   map.put("client_secret", SparkClient.sparkClientSecret);
			   map.put("grant_type", "authorization_code");
			   map.put("code", openIdSparkCode[0]);
			   map.put("redirect_uri", SparkClient.sparkCallbackURL);
			   
			   // TODO: move to SparkClient.authenticate()?
			   SparkSession sparkSession = null;
			   try
			   {
				   HttpPost post = new HttpPost(SparkClient.getSparkOAuth2GrantString());
				   SparkClient.initSparkHeader(post);
				   ObjectMapper mapper = new ObjectMapper();
				   StringEntity stringEntity = new StringEntity(mapper.writeValueAsString(map));
				   stringEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
				   post.setEntity(stringEntity);
				   HttpClient httpclient = new DefaultHttpClient(); 
				   HttpResponse response = httpclient.execute(post);
				   String responseBody = EntityUtils.toString(response.getEntity());
				   Log.d(TAG, "OAuth2 response>" + responseBody);
				   sparkSession = mapper.readValue(responseBody, SparkSession.class);
				   sparkClient.setSession(sparkSession);
				   Connection<Response> connection = sparkClient.getConnection();
				   ((ConnectionApacheHttp)connection).setHeaders(sparkClient.getHeaders());
				   
				   Map<ApiParameter,String> parameters = new HashMap<ApiParameter,String>();
				   parameters.put(ApiParameter._limit, "50");
				   parameters.put(ApiParameter._expand, "PrimaryPhoto");
				   parameters.put(ApiParameter._select, "ListingId,StreetNumber,StreetDirPrefix,StreetName,StreetDirSuffix,StreetSuffix,BedsTotal,BathsTotal,ListPrice,City,StateOrProvince");
				   parameters.put(ApiParameter._filter, "PropertyType Eq 'A'");
				   parameters.put(ApiParameter._orderby, "-ListPrice");

				   try
				   {
					   Response r = sparkClient.get("/listings",parameters);
					   Log.d(TAG, "success>" + r.isSuccess());
				   }
				   catch(FlexmlsApiClientException e)
				   {
					   Log.e(TAG, "/listings exception>", e);
				   }
			   } 
			   catch (Exception e)
			   {
				   Log.e(TAG, "OAuth2PostTask exception>", e);
			   }
	    	 
	    	 return sparkSession;
	     }
	     
	     protected void onPostExecute(SparkSession sparkSession) {
	    	 // TODO: set spark client on Application object
	    	 // TODO: close web view
	     }
	 }
}