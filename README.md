SparkAndroid Example App
=================================

Example Android app that makes use of `SparkJava` Spark API library to authenticate via Hybrid or OpenID methods, search listings, view listings, view an individual listing with photos and standard fields, and view a user account.  View app [screenshots](./Spark Android Screenshots.pdf) on a Nexus 7.

## Requirements

* JDK 1.6+
* [Maven 3.0.3+](http://maven.apache.org/download.html)
* [SparkJava](http://www.github.com/sparkapi/SparkJava)
* [Android SDK](http://developer.android.com/sdk/index.html) -- at least SDK Version 14 should be downloaded.
* *Optional* Eclipse IDE with [Android Development Tools](http://developer.android.com/tools/sdk/eclipse-adt.html) and [m2eclipse](http://eclipse.org/m2e/) installed
* App designed for devices running Android 4.0 or later (API level 14 / Ice Cream Sandwich)

## Configuration

Once you [register](http://www.sparkplatform.com/register/developers) as a Spark developer and receive your Spark Client Id and Client Secret, open the [sparkapi.properties](./res/raw/sparkapi.properties) file and set the `API_KEY` and `API_SECRET` properties.  You must also set the `USER_AGENT` with the name of your app or your API requests will not be accepted.

Once an Android SDK is [downloaded](http://developer.android.com/sdk/installing/adding-packages.html) to your development machine using the Android SDK tool, it must be deployed to your local maven repository using the [maven-android-sdk-deployer](https://github.com/mosabua/maven-android-sdk-deployer) and command `mvn install -P 4.0`.  The pom.xml is currently set to use the 4.0 SDK (Version 14) but more recent versions can also be used.

If you want to run on a virtual device, create an Android Virtual Device (AVD) using the `android avd` [tool](http://developer.android.com/tools/devices/index.html).   

Get a handle to a `SparkAPI` object via the `getInstance()` singleton static method.  If the instance has not been created, `SparkAPI` will be instantiated and returned with your configuration.

## Command Line Interface

The `pom.xml` file contains the maven configuration.  You must set the sdk path to the location of your local Android SDKs.  If you want to run on an android device, make sure the `<device>usb</device>` tag is present.  Otherwise, to run on the AVD, use the `<device>emulator</device>` tag. 

To compile, install, and run on an Android device, use the target `mvn clean install android:deploy android:run`.  

## Examples

### Authentication

The `SparkAPI` object from the `SparkJava` API library is designed to work with Android `WebView` and `WebViewClient` objects to initiate and process Spark authentication.

**Initiating an Authentication Request**:

* To initiate a Hybrid authentication request, call `WebView loadUrl()` with `getSparkHybridOpenIdURLString`.

* To initiate an OpenID authentication request, call `WebView loadUrl()` with `getSparkOpenIdURLString` or `getSparkOpenIdAttributeExchangeURLString`.

These authentication methods are typically placed in a `WebViewClient` object to respond to a URL request generated after the user provides their Spark credentials.  See [WebViewActivity.java](./src/com/sparkplatform/ui/WebViewActivity.java) for an example.

``` java
		public boolean shouldOverrideUrlLoading (WebView view, String url)
		{
		    String openIdSparkCode = null;
		    if(loginHybrid && (openIdSparkCode = SparkAPI.isHybridAuthorized(url)) != null)
		    {
				Log.d(TAG, "openIdSparkCode>" + openIdSparkCode);
				new OAuth2PostTask().execute(openIdSparkCode);	   				   
				return true;
		    }

		    return false;
		}
		
	private class OAuth2PostTask extends AsyncTask<String, Void, SparkSession> {
	     protected SparkSession doInBackground(String... openIdSparkCode) {
	    	 SparkSession session = null;
	    	 try
	    	 {
	    		 session = sparkClient.hybridAuthenticate(openIdSparkCode[0]);
	    	 }
	    	 catch(SparkAPIClientException e)
	    	 {
	    		 Log.e(TAG, "SparkApiClientException", e);
	    	 }
	    	 
	    	 return session;
	     }
	     
	     protected void onPostExecute(SparkSession sparkSession) {	    	 
	    	if(sparkSession != null)
	    	{
	    		processAuthentication(sparkSession, null);
	    		
	    		Intent intent = new Intent(getApplicationContext(), ViewListingsActivity.class);
	    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    		startActivity(intent);	  
	    	}
		 }
	 }
```

### Making API calls

Below is an example API call to the `/my/account` Spark API endpoint from the example app.  On response, the list view interface is updated.

``` java
	 private class MyAccountTask extends AsyncTask<Void, Void, Response> {
	     protected Response doInBackground(Void... v) {
				   
	    	 Response r = null;
	    	 try
	    	 {
	    		 r = SparkAPI.getInstance().get("/my/account",null);
	    	 }
	    	 catch(SparkAPIClientException e)
	    	 {
	    		 Log.e(TAG, "/my/account exception>", e);
	    	 }
	    	 
	    	 return r;
	     }
	     	     
	     protected void onPostExecute(Response r) {
	    	 JsonNode account = r.getFirstResult();
	    	 
	    	 if(account != null)
	    	 {
	    		 List<Map<String,String>> list = new ArrayList<Map<String,String>>();
	    		 ActivityHelper.addListLine(list, "Name", account.get("Name").getTextValue());
	    		 ActivityHelper.addListLine(list, "Office", account.get("Office").getTextValue());
	    		 ActivityHelper.addListLine(list, "Company", account.get("Company").getTextValue());
	    		 ActivityHelper.addArrayLine(account, "Addresses", "Address", list, "Address");
	    		 ActivityHelper.addListLine(list, "MLS", account.get("Mls").getTextValue());
	    		 ActivityHelper.addArrayLine(account, "Emails", "Address", list, "Email");
	    		 ActivityHelper.addArrayLine(account, "Phones", "Number", list, "Phone");
	    		 ActivityHelper.addArrayLine(account, "Websites", "Uri", list, "Website");

	    		 ListAdapter adapter = new SimpleAdapter(getApplicationContext(), 
	    				 list,
	    				 R.layout.two_line_list_item, 
	    				 new String[] {"line1", "line2"}, 
	    				 new int[] {android.R.id.text1, android.R.id.text2});
	    		 setListAdapter(adapter);
	    	 }
	     }
	 }
```

## Getting Started with your own App

The example app provides a great starting point for building your own Spark-powered Android app.  At a minimum, the core authentication features encapsulated by `LoginActivity` and `WebViewActivity` can be repurposed.

In your `MainActivity` `onCreate` method, you will need code similar to below that reads any saved tokens and bypasses Login if the session is valid to show your home `Activity`.  If the session is not valid, the `LoginActivity` is presented.

``` java
public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getMainIntent();
		startActivity(intent);
	}
	
	private Intent getMainIntent()
	{
		Intent intent = null;
		
		try
		{
			SecurePreferences p = new SecurePreferences(this,UIConstants.SPARK_PREFERENCES, SparkAPI.getConfiguration().getApiSecret(), false);
			String accessToken = p.getString(UIConstants.AUTH_ACCESS_TOKEN);
			String refreshToken = p.getString(UIConstants.AUTH_REFRESH_TOKEN);
			String openIdToken = p.getString(UIConstants.AUTH_OPENID);
			if(accessToken != null && refreshToken != null)
			{
				SparkSession session = new SparkSession();
				session.setAccessToken(accessToken);
				session.setRefreshToken(refreshToken);
				SparkAPI.getInstance().setSession(session);
				intent = new Intent(this, ViewListingsActivity.class);
			}
			else if(openIdToken != null)
			{
				SparkSession session = new SparkSession();
				session.setOpenIdToken(openIdToken);
				SparkAPI.getInstance().setSession(session);
				intent = new Intent(this, MyAccountActivity.class);
			}
			else
				intent = new Intent(this, LoginActivity.class);
		}
		catch(SparkAPIClientException e)
		{
			Log.e(TAG, "SparkApiClientException", e);
		}
		
		return intent;
	}
```

In `WebViewActivity`, the `processAuthentication` method should also be modified to save any session state (securely to SharedPreferences or other storage) as well as redirect the user to the top `Activity`.

``` java
	 private class OAuth2PostTask extends AsyncTask<String, Void, SparkSession> {

		...
	     
	     protected void onPostExecute(SparkSession sparkSession) {	    	 
	    	if(sparkSession != null)
	    	{
	    		processAuthentication(sparkSession, null);
	    		
	    		Intent intent = new Intent(getApplicationContext(), ViewListingsActivity.class);
	    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    		startActivity(intent);	  
	    	}
		 }
	 }
	 
	 private void processAuthentication(SparkSession session, String url)
	 {
		SecurePreferences p = new SecurePreferences(this,UIConstants.SPARK_PREFERENCES, SparkAPI.getConfiguration().getApiSecret(), false);
		editor.put(UIConstants.AUTH_ACCESS_TOKEN, session.getAccessToken());
		editor.put(UIConstants.AUTH_REFRESH_TOKEN, session.getRefreshToken());
	 }
```

## Dependencies

* [Apache commons-codec](http://commons.apache.org/codec/)
* [Apache commons-lang3](http://commons.apache.org/lang/)
* [Apache commons-logging](http://commons.apache.org/logging/)
* [Jackson JSON processor](http://jackson.codehaus.org/)
* [JodaTime](http://joda-time.sourceforge.net/)
* [log4j](http://logging.apache.org/log4j/1.2/)
* [android-logging-log4j](http://code.google.com/p/android-logging-log4j/)
* [SecurePreferences](https://github.com/sveinungkb/encrypted-userprefs)

## Compatibility

Tested OSs: Android 4.2 Jelly Bean (Version 17), Android 4.0 Ice Cream Sandwich (Version 14)

Tested Eclipse versions: 3.7 Indigo

Tested Devices: Nexus 7, Nexus 4
