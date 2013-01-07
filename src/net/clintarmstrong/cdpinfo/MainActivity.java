package net.clintarmstrong.cdpinfo;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	//Global Variables
	CDP cdp;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File yourFile = new File("/data/data/net.clintarmstrong.cdpinfo/files/tcpdump");
        if(!yourFile.exists()) {
        	new CopyTCPDump().execute();
        }
    }
    
    private class CopyTCPDump extends AsyncTask<Void, Void, Void> {
    	ProgressDialog dialog = new ProgressDialog(MainActivity.this);
    	
    	@Override
        protected void onPreExecute() {
            dialog.setMessage("Copying TCPDump Binary");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }
        
        @Override
        protected void onPostExecute(Void result) {
        	System.gc();
        	dialog.dismiss();
        }

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
        	try {
        		InputStream inputStream = getResources().openRawResource(R.raw.tcpdump);
    			FileOutputStream outputStream = openFileOutput("tcpdump", 0);
    			byte[] buffer = new byte[1024];
    			int length = 0;
                try {
    				while ((length = inputStream.read(buffer)) > 0) {
    				    outputStream.write(buffer, 0, length);
    				}
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		} catch (FileNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        	rootExec("chmod 755 /data/data/net.clintarmstrong.cdpinfo/files/tcpdump");
			return null;
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	//Handle Item selection
    	switch (item.getItemId())
    	{
    	case R.id.menu_settings:
    		break;
    	case R.id.menu_copy:
    		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
    		ClipData clip = ClipData.newPlainText("CDP", cdp.toString());
    		clipboard.setPrimaryClip(clip);
    	}
    	return true;
    }
    
    /** Called when the user clicks the Capture button */
    public void captureCDP(View view) {
    	new captureCDP().execute();
    }
    
    private class captureCDP extends AsyncTask<Void, Void, Void> {
    	ProgressDialog dialog = new ProgressDialog(MainActivity.this);
    	
    	@Override
        protected void onPreExecute() {
            dialog.setMessage("Waiting for CDP frame...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					cancel(true);
				}
            });
            dialog.show();
        }
        
        @Override
        protected void onPostExecute(Void result) {
        	dialog.dismiss();
        }

		@Override
		protected Void doInBackground(Void... params) {
			final String command = "/data/data/net.clintarmstrong.cdpinfo/files/tcpdump -i eth0 -nn -v -s 1500 -c 1 'ether[20:2] == 0x2000'";
			final String output = rootExec(command);
			cdp = parse(output);
			final TextView txtDeviceID = (TextView)findViewById(R.id.txtDeviceID);
			final TextView txtAddress = (TextView)findViewById(R.id.txtAddress);
			final TextView txtPortID = (TextView)findViewById(R.id.txtPortID);
			final TextView txtPlatform = (TextView)findViewById(R.id.txtPlatform);
			final TextView txtVLAN = (TextView)findViewById(R.id.txtVLAN);
			final TextView txtRaw = (TextView)findViewById(R.id.txtRaw);
			runOnUiThread(new Runnable() 
			{
				public void run()
				{
					txtDeviceID.setText(cdp.device_id);
					txtAddress.setText(cdp.address);
					txtPortID.setText(cdp.remote_port);
					txtPlatform.setText(cdp.platform);
					txtVLAN.setText(cdp.vlan_id);
					txtRaw.setText(output);
				}
			});
        	return null;
		}
    }
    CDP parse(final String input)
    {
    	CDP data = new CDP();
    	
    	//find Device ID
    	Pattern p = Pattern.compile("Device-ID.*: '(.*)'");
    	Matcher m = p.matcher(input);
    	if(m.find())
    		data.device_id = m.group(1);

    	//find IP Address
    	p = Pattern.compile("Address.*?:.*?(\\d+\\.\\d+\\.\\d+\\.\\d+)");
    	m = p.matcher(input);
    	if(m.find())
    		data.address = m.group(1);
    	
    	//find Platform
    	p = Pattern.compile("Platform.*?: '(.*?)'");
    	m = p.matcher(input);
    	if(m.find())
    		data.platform = m.group(1);
    	
    	//find Port ID
    	p = Pattern.compile("Port-ID.*?: '(.*?)'");
    	m = p.matcher(input);
    	if(m.find())
    		data.remote_port = m.group(1);
    	
    	//find VLAN ID
    	p = Pattern.compile("Native VLAN ID.*: (\\d+)");
    	m = p.matcher(input);
    	if(m.find())
    		data.vlan_id = m.group(1);
    	
    	//return data
    	return data;
    }
    // Execute Command as Root
    // returns: stdout from the command that ran or string beginning with ERROR: 
    String rootExec(final String command)
    {
    	Runtime runtime = Runtime.getRuntime();
	    Process proc = null;
	    OutputStreamWriter osw = null;
	    // Running the Script
	    try
	    {
	    	proc = runtime.exec("su");
		    osw = new OutputStreamWriter(proc.getOutputStream());
		    osw.write(command);
		    osw.flush();
		    osw.close();
	    }
	    catch (IOException ex)
	    {
	    	// Log error
		    Log.e("rootExec()", "Command resulted in an IO Exception: " + command);
		    return "ERROR: Command resulted in an IO Exception: " + command;
	    }
	    finally
	    {
	    	if (osw != null)
		    {
	    		try
	    		{
	    			osw.close();
	    		}
	    		catch (IOException e)
	    		{
	    			Log.e("rootExec()", "Unable to close output stream.");
	    		}
		    }
	    }
	    try 
	    {
	    	proc.waitFor();
	    }
	    catch (InterruptedException e)
	    {
	    	Log.e("rootExec()", "Process Interrupted.");
	    	return "ERROR: Interrupt Detected";
	    }
	    if (proc.exitValue() != 0)
	    {
	    	Log.e("rootExec()", "Command returned error: " + command + "\n Exit code: " + proc.exitValue());
	    	return "ERROR: " + command + "\n Exit code: " + proc.exitValue();
	    }
	    else
	    {
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream())); 
		    int read;
	    	char[] buffer = new char[4096];
	    	StringBuffer output = new StringBuffer();
	    	try
	    	{
	    		while ((read = reader.read(buffer)) > 0)
	    		{
	    			output.append(buffer, 0, read);
	    		}
		    } 
	    	catch (IOException e) 
	    	{
	    		Log.e("rootExec()", "Unable to parse command output\n " + output.toString());
	    		return "ERROR: Unable to parse command output\n" + output.toString();
		    }
	    	String exit = output.toString();
		    if(exit != null && exit.length() == 0)
		    {
		    	exit = "ERROR: Command executed successfully but no output was generated";
		    } 
		    return exit;
	    }
    }
}
