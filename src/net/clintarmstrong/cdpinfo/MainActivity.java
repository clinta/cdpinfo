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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ShareActionProvider;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	//Global Variables
	CDP cdp;
	private ShareActionProvider mShareActionProvider;
	
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
        
        MenuItem menu_share = menu.findItem(R.id.menu_share);
        mShareActionProvider = (ShareActionProvider)menu_share.getActionProvider();
        
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
			//final String output = rootExec(command);
			final String output = "08:23:14.913275 CDPv2, ttl: 180s, checksum: 692 (unverified), length 486\n      Device-ID (0x01), length: 38 bytes: 'C2960S-48LPS-SCORP-M1-5.secantcorp.com'\n     Version String (0x05), length: 248 bytes: \n          Cisco IOS Software, C2960S Software (C2960S-UNIVERSALK9-M), Version 12.2(55)SE1, RELEASE SOFTWARE (fc1)\n         Technical Support: http://www.cisco.com/techsupport\n    Copyright (c) 1986-2010 by Cisco Systems, Inc.\n    Compiled Thu 02-Dec-10 08:43 by prod_rel_team\n Platform (0x06), length: 23 bytes: 'cisco WS-C2960S-48LPS-L'\n  Address (0x02), length: 13 bytes: IPv4 (1) 10.11.252.5\n   Port-ID (0x03), length: 21 bytes: 'GigabitEthernet1/0/39'\n     Capability (0x04), length: 4 bytes: (0x00000028): L2 Switch, IGMP snooping\n      Protocol-Hello option (0x08), length: 32 bytes: \n   VTP Management Domain (0x09), length: 10 bytes: 'SECANTCORP'\n      Native VLAN ID (0x0a), length: 2 bytes: 12\n   Duplex (0x0b), length: 1 byte: full\n   ATA-186 VoIP VLAN request (0x0e), length: 3 bytes: app 1, vlan 19\n AVVID trust bitmap (0x12), length: 1 byte: 0x00\n AVVID untrusted ports CoS (0x13), length: 1 byte: 0x01\n    Management Addresses (0x16), length: 13 bytes: IPv4 (1) 10.11.252.5\n  unknown field type (0x1a), length: 12 bytes: \n      0x0000:  0000 0001 0000 0000 ffff ffff";
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
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_TEXT, cdp.toString());
			shareIntent.setType("text/plain");
			if(mShareActionProvider != null)
			{
				mShareActionProvider.setShareIntent(shareIntent);
			}
			
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
