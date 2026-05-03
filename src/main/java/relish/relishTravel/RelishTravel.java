package relish.relishTravel;

import org.bukkit.plugin.java.JavaPlugin;
import relish.relishTravel.command.LaunchCommand;
import relish.relishTravel.command.RelishTravelCommand;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.handler.ChargeManager;
import relish.relishTravel.handler.ElytraHandler;
import relish.relishTravel.handler.LaunchHandler;
import relish.relishTravel.handler.PersistenceHandler;
import relish.relishTravel.handler.BoostHandler;
import relish.relishTravel.handler.SpeedDisplayHandler;
import relish.relishTravel.listener.BoostListener;
import relish.relishTravel.listener.ChargeListener;
import relish.relishTravel.listener.ElytraProtectionListener;
import relish.relishTravel.listener.LaunchListener;
import relish.relishTravel.listener.PersistenceListener;
import relish.relishTravel.listener.SafetyListener;
import relish.relishTravel.listener.UpdateNotifyListener;
import relish.relishTravel.message.MessageManager;
import relish.relishTravel.util.UpdateChecker;

public class RelishTravel extends JavaPlugin {
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private ChargeManager chargeManager;
    private ElytraHandler elytraHandler;
    private LaunchHandler launchHandler;
    private BoostHandler rightClickBoostHandler;
    private SpeedDisplayHandler speedDisplayHandler;
    private PersistenceHandler persistenceHandler;
    private UpdateChecker updateChecker;
    
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        try {
            this.configManager = new ConfigManager(this);
            configManager.loadConfig();
            debug("ConfigManager initialized and config loaded");
            
            this.messageManager = new MessageManager(this, configManager);
            debug("MessageManager initialized");
            
            this.chargeManager = new ChargeManager(this);
            debug("ChargeManager initialized");
            
            this.elytraHandler = new ElytraHandler(this, configManager);
            debug("ElytraHandler initialized");
            
            this.launchHandler = new LaunchHandler(this, configManager, messageManager, chargeManager, elytraHandler);
            debug("LaunchHandler initialized");
            
            this.rightClickBoostHandler = new BoostHandler(this, configManager, messageManager, launchHandler);
            debug("BoostHandler initialized");
            
            this.speedDisplayHandler = new SpeedDisplayHandler(this, messageManager, launchHandler, rightClickBoostHandler);
            debug("SpeedDisplayHandler initialized");
            
            this.persistenceHandler = new PersistenceHandler(this, configManager, launchHandler, elytraHandler);
            debug("PersistenceHandler initialized");
            
            messageManager.loadMessages();
            debug("Messages loaded");
            
            registerListeners();
            registerCommands();
            registerDynamicPermissions();
            
            getServer().getScheduler().runTaskTimer(this, () -> {
                if (persistenceHandler != null) {
                    persistenceHandler.cleanupExpiredStates();
                }
            }, 6000L, 6000L);
            debug("Cleanup scheduler started");
            
            this.updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates();
            
            long loadTime = System.currentTimeMillis() - startTime;
            printStartupBanner(loadTime);
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            if (isDebugMode()) {
                e.printStackTrace();
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        String version = getDescription().getVersion();
        
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
        getLogger().info("\u001b[1;96m  RELISH TRAVEL  \u001b[0m\u001b[90mv" + version + "\u001b[0m");
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
        
        try {
            if (chargeManager != null) {
                getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[90mClearing charge data...\u001b[0m");
                chargeManager.cleanup();
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up ChargeManager: " + e.getMessage());
        }
        
        try {
            if (elytraHandler != null) {
                getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[90mShutting down elytra handler...\u001b[0m");
                elytraHandler.cleanup();
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up ElytraHandler: " + e.getMessage());
        }
        
        try {
            if (speedDisplayHandler != null) {
                getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[90mStopping speed displays...\u001b[0m");
                speedDisplayHandler.cleanup();
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up SpeedDisplayHandler: " + e.getMessage());
        }
        
        try {
            if (launchHandler != null) {
                getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[90mClearing launch states...\u001b[0m");
                launchHandler.cleanup();
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up LaunchHandler: " + e.getMessage());
        }
        
        try {
            if (rightClickBoostHandler != null) {
                getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[90mShutting down boost handler...\u001b[0m");
                rightClickBoostHandler.cleanup();
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up BoostHandler: " + e.getMessage());
        }
        
        try {
            if (persistenceHandler != null) {
                getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[90mSaving persistence data...\u001b[0m");
                persistenceHandler.cleanup();
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up PersistenceHandler: " + e.getMessage());
        }
        
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
        getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[91mDisabled\u001b[0m");
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
    }
    
    private void registerListeners() {
        ChargeListener chargeListener = new ChargeListener(this, chargeManager, configManager, messageManager);
        LaunchListener launchListener = new LaunchListener(this, launchHandler, configManager, messageManager);
        SafetyListener safetyListener = new SafetyListener(this, chargeManager);
        BoostListener boostListener = new BoostListener(this, rightClickBoostHandler);
        ElytraProtectionListener elytraProtectionListener = new ElytraProtectionListener(this, launchHandler);
        PersistenceListener persistenceListener = new PersistenceListener(this, persistenceHandler);
        UpdateNotifyListener updateNotifyListener = new UpdateNotifyListener(this);
        relish.relishTravel.listener.AchievementListener achievementListener = new relish.relishTravel.listener.AchievementListener(this, configManager);
        
        getServer().getPluginManager().registerEvents(chargeListener, this);
        getServer().getPluginManager().registerEvents(launchListener, this);
        getServer().getPluginManager().registerEvents(safetyListener, this);
        getServer().getPluginManager().registerEvents(boostListener, this);
        getServer().getPluginManager().registerEvents(elytraProtectionListener, this);
        getServer().getPluginManager().registerEvents(persistenceListener, this);
        getServer().getPluginManager().registerEvents(updateNotifyListener, this);
        getServer().getPluginManager().registerEvents(achievementListener, this);
        debug("Registered 8 listeners");
    }
    
    private void registerCommands() {
        RelishTravelCommand rtCommand = new RelishTravelCommand(this, configManager, messageManager);
        LaunchCommand rtlCommand = new LaunchCommand(this, configManager, messageManager);
        
        getCommand("relishtravel").setExecutor(rtCommand);
        getCommand("relishtravel").setTabCompleter(rtCommand);
        getCommand("rtl").setExecutor(rtlCommand);
        getCommand("rtl").setTabCompleter(rtlCommand);
        debug("Registered commands successfully");
    }
    
    public void reload() {
        configManager.loadConfig();
        messageManager.loadMessages();
        if (chargeManager != null) {
            chargeManager.updateConfigCache();
        }
        if (speedDisplayHandler != null) {
            speedDisplayHandler.onConfigReload();
        }
        registerDynamicPermissions();
    }
    
    private void registerDynamicPermissions() {
        java.util.Map<String, Integer> boostLimits = configManager.getBoostPermissionLimits();
        for (String permissionName : boostLimits.keySet()) {
            if (getServer().getPluginManager().getPermission(permissionName) == null) {
                org.bukkit.permissions.Permission perm = new org.bukkit.permissions.Permission(
                    permissionName, 
                    "Boost limit: " + boostLimits.get(permissionName), 
                    org.bukkit.permissions.PermissionDefault.FALSE
                );
                getServer().getPluginManager().addPermission(perm);
                debug("Registered dynamic permission: " + permissionName);
            }
        }
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public ChargeManager getChargeManager() {
        return chargeManager;
    }
    
    public ElytraHandler getElytraHandler() {
        return elytraHandler;
    }
    
    public LaunchHandler getLaunchHandler() {
        return launchHandler;
    }
    
    public SpeedDisplayHandler getSpeedDisplayHandler() {
        return speedDisplayHandler;
    }
    
    public BoostHandler getRightClickBoostHandler() {
        return rightClickBoostHandler;
    }
    
    public PersistenceHandler getPersistenceHandler() {
        return persistenceHandler;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    private void printStartupBanner(long loadTime) {
        String version = getDescription().getVersion();
        String timeString = loadTime < 1000 ? loadTime + "ms" : String.format("%.2fs", loadTime / 1000.0);
        
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
        getLogger().info("\u001b[1;96m  RELISH TRAVEL  \u001b[0m\u001b[90mv" + version + "\u001b[0m");
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
        getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[97mStatus: \u001b[0m\u001b[92mEnabled\u001b[0m");
        getLogger().info("\u001b[96m  ▶ \u001b[0m\u001b[97mLoad Time: \u001b[0m\u001b[96m" + timeString + "\u001b[0m");
        getLogger().info("\u001b[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001b[0m");
    }
    
    public void debug(String message) {
        if (configManager != null && configManager.isDebugMode()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    public boolean isDebugMode() {
        return configManager != null && configManager.isDebugMode();
    }
}
