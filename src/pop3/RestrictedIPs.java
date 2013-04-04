package pop3;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.net.util.SubnetUtils;

public class RestrictedIPs {
	private static Set<InetAddress> ipsRestricted = new HashSet<InetAddress>();
	private static Set<SubnetUtils> subNetsRestricted = new HashSet<SubnetUtils>();

	public static boolean isBanned(InetAddress ip) {
		if (ipsRestricted.contains(ip)) {
			return true;
		}
		for (SubnetUtils subNet : subNetsRestricted) {
			if (subNet.getInfo().isInRange(ip.getHostAddress())) {
				return true;
			}
		}
		return false;
	}

	public static boolean banIPByAddress(String ip) {
		try {
			ipsRestricted.add(InetAddress.getByName(ip));
			System.out.println("Baneada IP: "+ip);
			return true;
		} catch (Exception e) {
			System.out.println("Warnign: No se pudo banear la IP "+ip);
		}
		return false;
	}

	public static boolean banIPByName(String name) {
		return banIPByAddress(name);
	}

	public static boolean banIPBySubnet(String cidrNotation) {
		try {
			subNetsRestricted.add(new SubnetUtils(cidrNotation));
			System.out.println("Baneada subret: "+cidrNotation);
			return true;
		} catch (Exception e) {
			System.err.println("Warnign: No se pudo banear la subred:" + cidrNotation);
		}
		return false;
	}

	public static boolean banIPBySubnet(String address, String mask) {
		try {
			subNetsRestricted.add(new SubnetUtils(address, mask));
			System.out.println("Baneada subret: "+ address + "(sumascara "+mask+")");
			return true;
		} catch (Exception e) {
			System.out.println("Warnign: No se pudo bannear la subred:"+address + "(sumascara "+mask+")");
		}
		return false;
	}
	
	public static boolean unbanIPByAddress(String ip){
		try {
			if( ipsRestricted.remove(InetAddress.getByName(ip))){
				System.out.println("IP "+ip+" desbloqueada.");
				return true;
			}
			System.out.println("Warnign: La IP "+ip+" no esta banneada.");
			return false;
		} catch (Exception e) {
			System.out.println("Warnign: No se pudo desbloquear la IP "+ip);
		}
		return false;
	}
	
	public static boolean unbanIPByHostname(String host){
		return unbanIPByAddress(host);
	}
	
	public static boolean unbanIPBySubnet(String cidrNotation){
		try {
			if( subNetsRestricted.remove(new SubnetUtils(cidrNotation)) ){
				System.out.println("Subred "+cidrNotation+" desbloqueada.");
				return true;
			}
			System.out.println("Warnign: La subred "+cidrNotation+" no esta banneada.");
			return false;
		} catch (Exception e) {
			System.out.println("Warnign: No se pudo desbloquear la subred "+cidrNotation);
		}
		return false;
	}
	
	public static boolean unbanIPBySubnet(String address, String mask){
		try {
			if( subNetsRestricted.remove(new SubnetUtils(address, mask)) ){
				System.out.println("Subred "+address + "(sumascara "+mask+")"+" desbloqueada.");
				return true;
			}
			System.out.println("Warnign: La subred "+address + "(sumascara "+mask+")"+" no esta banneada.");
			return false;
		} catch (Exception e) {
			System.out.println("Warnign: No se pudo desbloquear la subred "+address + "(sumascara "+mask+")");
		}
		return false;
	}
	
	public static void unbanAll(){
		subNetsRestricted.clear();
		ipsRestricted.clear();
	}
}
