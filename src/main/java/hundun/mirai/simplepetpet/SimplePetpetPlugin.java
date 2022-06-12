package hundun.mirai.simplepetpet;

import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;

/**
 * @author hundun
 * Created on 2021/08/09
 */
public class SimplePetpetPlugin extends JavaPlugin {
    public static final SimplePetpetPlugin INSTANCE = new SimplePetpetPlugin(); 

    public SimplePetpetPlugin() {
        super(new JvmPluginDescriptionBuilder(
                "hundun.mirai.simplepetpet",
                "0.1.0"
            )
            .build());
    }
    
    @Override
    public void onEnable() {
        getLogger().info("SimplePetpetPlugin onEnable");
        
        CommandManager.INSTANCE.registerCommand(new PetpetCommand(this), false);
    }

}
