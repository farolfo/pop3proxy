package initialConf.jaxb;

import initialConf.AppConf;
import initialConf.jaxb.XMLAppConf.AuxAdministrator;
import initialConf.jaxb.XMLAppConf.AuxRestrictedIPs.Subnet;
import initialConf.jaxb.XMLAppConf.AuxUser;
import initialConf.jaxb.XMLAppConf.AuxUser.AuxDelRestrictions.AuxFrom;
import initialConf.jaxb.XMLAppConf.AuxUser.TimesToLogin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import pop3.RestrictedIPs;
import pop3.User;
import pop3.restriction.AttachmentTypeRestriction;
import pop3.restriction.DateRestriction;
import pop3.restriction.FromRestriction;
import pop3.restriction.HeadersRestriction;
import pop3.restriction.Restriction;
import pop3.restriction.SizeRestriction;
import pop3.restriction.StructureRestriction;

public class XMLParser implements AppConf {

	XMLAppConf conf;

	public boolean loadSettings() {
		try {
			JAXBContext context = JAXBContext.newInstance(XMLAppConf.class);
			conf = (XMLAppConf) context.createUnmarshaller().unmarshal(
					new File("conf.xml"));
			if (getDefaultServer() == null) {
				System.out
						.println("Error: falta indicar server default en el xml.");
				return false;
			}
		} catch (JAXBException e) {
			System.out
					.println("Error al parsear el XML en la configuracion inicial.");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public Map<String, User> getUsersMap() {
		Map<String, User> ans = new HashMap<String, User>();
		if (conf.users == null) {
			return ans;
		}
		for (AuxUser u : conf.users) {
			if (u.username != null) {
				User user = new User(u.username);
				if (u.countLoginsPerDay != null) {
					user.setMaxConnections(u.countLoginsPerDay);
				}
				if (u.server != null) {
					user.setServer(u.server);
				}
				if (u.timesToLogin != null) {
					for (TimesToLogin t : u.timesToLogin) {
						user.addUnallowedConnectionInterval(t.hourFrom,
								t.hourTo);
					}
				}
				if (u.delRestrictions != null) {
					if (u.delRestrictions.from != null) {
						for (AuxFrom from : u.delRestrictions.from) {
							if (from.email != null) {
								// Si existe el mail, banea ese mail con
								// exactMatch.
								// Si no banea el username sin exactMatch
								user.addRestriction(new FromRestriction(
										from.email, true));
							} else if (from.username != null) {
								user.addRestriction(new FromRestriction(
										from.username, false));
							}
						}
					}
					if (u.delRestrictions.ctype != null) {
						for (String type : u.delRestrictions.ctype) {
							if (type != null && !type.equals("")) {
								user.addRestriction(new AttachmentTypeRestriction(
										type));
							}
						}
					}
					if (u.delRestrictions.size != null) {
						for (String s : u.delRestrictions.size) {
							String[] part = s.split(" ");
							if (part.length >= 2) {
								try {
									user.addRestriction(new SizeRestriction(
											part[0], Integer.valueOf(part[1])));
								} catch (Exception e) {
									System.out
											.println("Couldn't parse size restriction");
								}
							}
						}
					}
					if (u.delRestrictions.date != null) {
						for (String s : u.delRestrictions.size) {
							String[] part = s.split(" ");
							if (part.length >= 2) {
								SimpleDateFormat sdf = new SimpleDateFormat(
										"dd/MM/yyyy", Locale.US);
								Calendar c = Calendar.getInstance();
								try {
									c.setTime(sdf.parse(part[1]));
									user.addRestriction(new DateRestriction(
											part[0], c));
								} catch (Exception e) {
									System.out
											.println("Couldn't parse date restriction");
								}
							}
						}

					}

					// @XmlElement
					// String messageStructure;

					if (u.delRestrictions.messageStructure != null) {
						user.addRestriction(new StructureRestriction(
								u.delRestrictions.messageStructure));
					}

					if (u.delRestrictions.headerPatterns != null) {
						for (String s : u.delRestrictions.headerPatterns) {
							if (s != null) {
								String[] part = s.split(" ");
								if (part.length >= 3) {
									try {
										user.addRestriction(new HeadersRestriction(
												part[0]/* modifier */,
												part[1]/* header */, part[2]/* value */));
									} catch (Exception e) {
										System.out
												.println("Couldn't parse header restriction");
									}
								}
							}
						}
					}
				}
				ans.put(u.username, user);
			}
		}
		return ans;
	}

	@Override
	public String getTransformerPath() {
		if (conf.transformation != null && !conf.transformation.equals("")) {
			return conf.transformation;
		}
		return null;
	}

	@Override
	public List<Restriction> getGlobalRestrictions() {
		List<Restriction> ans = new LinkedList<Restriction>();
		if (conf.globalDelRest != null) {
			if (conf.globalDelRest.from != null) {
				for (AuxFrom from : conf.globalDelRest.from) {
					if (from.email != null) {
						// Si existe el mail, banea ese mail con
						// exactMatch.
						// Si no banea el username sin exactMatch
						ans.add(new FromRestriction(from.email, true));
					} else if (from.username != null) {
						ans.add(new FromRestriction(from.username, false));
					}
				}
			}
			if (conf.globalDelRest.ctype != null) {
				for (String type : conf.globalDelRest.ctype) {
					if (type != null && !type.equals("")) {
						ans.add(new AttachmentTypeRestriction(type));
					}
				}
			}
			if (conf.globalDelRest.size != null) {
				for (String s : conf.globalDelRest.size) {
					String[] part = s.split(" ");
					if (part.length >= 2) {
						try {
							ans.add(new SizeRestriction(part[0], Integer
									.valueOf(part[1])));
						} catch (Exception e) {
							System.out
									.println("Couldn't parse size restriction");
						}
					}
				}
			}
			if (conf.globalDelRest.date != null) {
				for (String s : conf.globalDelRest.size) {
					String[] part = s.split(" ");
					if (part.length >= 2) {
						SimpleDateFormat sdf = new SimpleDateFormat(
								"dd/MM/yyyy", Locale.US);
						Calendar c = Calendar.getInstance();
						try {
							c.setTime(sdf.parse(part[1]));
							ans.add(new DateRestriction(part[0], c));
						} catch (Exception e) {
							System.out
									.println("Couldn't parse date restriction");
						}
					}
				}
			}
		}
		return ans;
	}

	@Override
	public String getDefaultServer() {
		return conf.defaultServer;
	}

	@Override
	public void loadRestrictedIPs() {
		if (conf.restrictedIPs == null) {
			return;
		}
		if (conf.restrictedIPs.hostname != null) {
			for (String hostname : conf.restrictedIPs.hostname) {
				RestrictedIPs.banIPByName(hostname);
			}
		}
		if (conf.restrictedIPs.ips != null) {
			for (String ip : conf.restrictedIPs.ips) {
				RestrictedIPs.banIPByAddress(ip);
			}
		}
		if (conf.restrictedIPs.subnetCidr != null) {
			for (String sn : conf.restrictedIPs.subnetCidr) {
				RestrictedIPs.banIPBySubnet(sn);
			}
		}
		if (conf.restrictedIPs.subnet != null) {
			for (Subnet sn : conf.restrictedIPs.subnet) {
				if (sn.address != null && sn.submask != null) {
					RestrictedIPs.banIPBySubnet(sn.address, sn.submask);
				}
			}
		}
	}

	@Override
	public Map<String, String> getAdministratorsMap() {
		Map<String, String> ans = new HashMap<String, String>();
		if (conf.administrators == null
				|| conf.administrators.administrators == null) {
			return ans;
		}
		for (AuxAdministrator a : conf.administrators.administrators) {
			if (a.username != null && a.password != null) {
				ans.put(a.username, a.password);
			}
		}
		return ans;
	}

}
