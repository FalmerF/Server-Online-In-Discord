package net.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class CoreServerOnline extends JavaPlugin implements Listener {
	URLClassLoader childClassLoader;
	public static String textPattern;
	public static List<String> comamndsToSend = new ArrayList<String>();
	
	@Override
    public void onEnable() {
		PluginManager pluginManager = Bukkit.getPluginManager();
		pluginManager.registerEvents((Listener) this, this);
		org.apache.logging.log4j.core.Logger logger;
        logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.addFilter(new Log4jFilter());
        
		try {
            final File[] libs = new File[] {new File(getDataFolder(), "libs/annotations.jar"),
                    new File(getDataFolder(), "libs/commons-collections4-4.4.jar"),
                    new File(getDataFolder(), "libs/conscrypt-openjdk.jar"),
                    new File(getDataFolder(), "libs/jackson-annotations-2.13.0-rc1.jar"),
                    new File(getDataFolder(), "libs/jackson-core-2.13.0-rc1.jar"),
                    new File(getDataFolder(), "libs/jackson-databind-2.13.0-rc1.jar"),
                    new File(getDataFolder(), "libs/JDA-4.3.0_277.jar"),
                    new File(getDataFolder(), "libs/kotlin-stdlib.jar"),
                    new File(getDataFolder(), "libs/kotlin-stdlib-common.jar"),
                    new File(getDataFolder(), "libs/nv-websocket-client.jar"),
                    new File(getDataFolder(), "libs/okhttp.jar"),
                    new File(getDataFolder(), "libs/okio.jar"),
                    new File(getDataFolder(), "libs/slf4j-api.jar"),
                    new File(getDataFolder(), "libs/trove-3.0.2.jar")};
            for (final File lib : libs) {
                if (!lib.exists()) {
                    extractFromJar(lib.getName(),
                            lib.getAbsolutePath());
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
		loadLibraries();
    }
	
	public static String getPluginFolderPath() {
		try {
			return new File(CoreServerOnline.class.getProtectionDomain().getCodeSource().getLocation()
			    .toURI()).getParentFile().getPath()+"/DiscordOnline/";
		} catch(Exception e) {
			return "";
		}
	}
	
	public static boolean extractFromJar(final String fileName,
            final String dest) throws IOException {
        if (getRunningJar() == null) {
            return false;
        }
        final File file = new File(dest);
        if (file.isDirectory()) {
            file.mkdir();
            return false;
        }
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
 
        final JarFile jar = getRunningJar();
        final Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            final JarEntry je = e.nextElement();
            if (!je.getName().contains(fileName)) {
                continue;
            }
            final InputStream in = new BufferedInputStream(
                    jar.getInputStream(je));
            final OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(file));
            copyInputStream(in, out);
            jar.close();
            return true;
        }
        jar.close();
        return false;
    }
	
	public static URL getJarUrl(final File file) throws IOException {
        return new URL("jar:" + file.toURI().toURL().toExternalForm() + "!/");
    }
	
	private static boolean RUNNING_FROM_JAR = false;
 
    public static JarFile getRunningJar() throws IOException {
        if (!RUNNING_FROM_JAR) {
            return null; // null if not running from jar
        }
        String path = new File(CoreServerOnline.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getAbsolutePath();
        path = URLDecoder.decode(path, "UTF-8");
        return new JarFile(path);
    }
    
    static {
        final URL resource = CoreServerOnline.class.getClassLoader()
                .getResource("plugin.yml");
        if (resource != null) {
            RUNNING_FROM_JAR = true;
        }
    }
    
    private final static void copyInputStream(final InputStream in,
            final OutputStream out) throws IOException {
        try {
            final byte[] buff = new byte[4096];
            int n;
            while ((n = in.read(buff)) > 0) {
                out.write(buff, 0, n);
            }
        } finally {
            out.flush();
            out.close();
            in.close();
        }
    }
	
	public void loadLibraries() {
		try {
			File dir = new File(getPluginFolderPath()+"/libs");
			dir.mkdir();
			
			List<URL> urls = new ArrayList<URL>();
			for(File file : dir.listFiles()) {
				if(file.getName().endsWith(".jar")) {
					urls.add(file.toURI().toURL());
				}
			}
			urls.add(CoreServerOnline.class.getProtectionDomain().getCodeSource().getLocation().toURI().toURL());
			childClassLoader = URLClassLoader.newInstance(urls.toArray(new URL[] {}), Bukkit.class.getClassLoader());
			Class c = childClassLoader.loadClass(this.getClass().getName());
			c.getDeclaredMethod("startBot").invoke(c);
		} catch(Exception e) {
			e.printStackTrace();
			getLogger().info(e.getMessage());
		}
	}
	
	public static void startBot() {
		try {
		    String token = "";
		    int delay = 6000;
		    textPattern = "{online}/{max_online}";
		    File configFile = new File(getPluginFolderPath()+"Config.cfg");
		    if(configFile.exists()) {
		    	FileReader Fr =  new FileReader(configFile);
		        final BufferedReader Br = new BufferedReader(Fr);
		        String line = "";
		        while ((line = Br.readLine()) != null) {
		        	try {
			        	if(!line.startsWith("//")) {
		       		 		String[] key = line.split("=", 2);
		       		 		if(key.length == 2) { 
		       		 			if(key[0].equalsIgnoreCase("delay"))
		       		 				delay = Integer.parseInt(key[1]);
		       		 			else if(key[0].equalsIgnoreCase("pattern"))
		       		 			textPattern = key[1];
		       		 			else if(key[0].equalsIgnoreCase("token"))
		       		 			token = key[1];
		       		 		}
			       		}
		        	} catch(Exception e) {
		        		Bukkit.getLogger().info("[SOID] ERROR Bad Parametr: " + line);
		        	}
		        }
	            Br.close();
		    }
		    else
		    	makeConfigFile(configFile);
		    
		    if(token.equals("")) {
		    	Bukkit.getLogger().info("[SOID] ERROR Bad API Key");
		    	return;
		    }
		    
		    JDABuilder builder = JDABuilder.createDefault(token);
		    builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
		    JDA bot = builder.build();
		    
		    bot.addEventListener(new MessageListener());
		    
		    Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bukkit.getPluginManager().getPlugin("DiscordOnline"), new  Runnable(){
				public void run(){
					int online = Bukkit.getOnlinePlayers().size();
					int maxOnline = Bukkit.getMaxPlayers();
					String activityText = textPattern.replaceAll("\\{online\\}", String.valueOf(online)).replaceAll("\\{max_online\\}", String.valueOf(maxOnline));
					bot.getPresence().setActivity(Activity.playing(activityText));
				}
			}, 1l, delay);
		    
	        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Bukkit.getPluginManager().getPlugin("DiscordOnline"), new  Runnable(){
				public void run(){
					while(comamndsToSend.size() > 0) {
						String command = comamndsToSend.remove(0);
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
					}
				}
			}, 1l, 20l);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void makeConfigFile(File configFile) {
		try {
			configFile.createNewFile();
			
			FileWriter fw = new FileWriter(configFile);
            final BufferedWriter bw2 = new BufferedWriter(fw);
            
            String text = "// Discord Online\n\n"
            		+ "// Discord Bot Token, get it at Discord Developer Portal (https://discord.com/developers/applications)\n"
            		+ "token=\n\n"
            		+ "// Delay Between Online Updates (in server ticks, 20 ticks = 1 second)\n"
            		+ "delay=6000\n\n"
            		+ "// Text Pattern. Use: {online} - online users; {max_online} - server max. online.\n"
            		+ "pattern={online}/{max_online}";
            
			bw2.write(text);
			bw2.close();
		} catch(Exception e) {
			
		}
	}
}
