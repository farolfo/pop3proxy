package initialConf;

import java.util.List;
import java.util.Map;

import pop3.User;
import pop3.restriction.Restriction;

public interface AppConf {
	public boolean loadSettings();
	
	public Map<String, User> getUsersMap();

	public String getTransformerPath();

	public List<Restriction> getGlobalRestrictions();
	
	public String getDefaultServer();
	
	public void loadRestrictedIPs();
	
	public Map<String, String> getAdministratorsMap();
}
