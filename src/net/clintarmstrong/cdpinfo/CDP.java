package net.clintarmstrong.cdpinfo;

public class CDP {
	public String device_id;
	public String address;
	public String remote_port;
	public String platform;
	
	public CDP()
	{
		device_id = new String();
		address = new String();
		remote_port = new String();
		platform = new String();
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Device-ID:\t" + device_id + "\n");
		sb.append("Address:\t" + address + "\n");
		sb.append("Remote Port:\t" + remote_port + "\n");
		sb.append("Platform:\t" + platform + "\n");
		return sb.toString();
	}
}
