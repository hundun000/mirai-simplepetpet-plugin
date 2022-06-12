package hundun.mirai.simplepetpet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import kotlin.Pair;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.CompositeCommand;
import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext;
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;
import xmmt.dituon.share.BasePetService;
import xmmt.dituon.share.ConfigDTO;
import xmmt.dituon.share.ImageSynthesis;
import xmmt.dituon.share.TextExtraData;

public class PetpetCommand extends CompositeCommand {
    
    private final BasePetService petService;
    private final JvmPlugin plugin;
    
    public PetpetCommand(JvmPlugin plugin) {
        super(plugin, "制图", new String[]{}, "我是PetpetCommand", plugin.getParentPermission(), CommandArgumentContext.EMPTY);
        this.plugin = plugin;
        this.petService = new BasePetService();
        initPetService(plugin);
    }
    
    /**
     * - 构造固定的ConfigDTO。若开发者有余力，可改为使用 {@link net.mamoe.mirai.console.data.java.JavaAutoSavePluginConfig JavaAutoSavePluginConfig} 的方式。<br>
     * - 以相对于插件数据目录的方式读取petService所需data目录。
     */
    private void initPetService(JvmPlugin plugin) {
        ConfigDTO config = new ConfigDTO();
        petService.readConfig(config);
        
        File petDataFolder = plugin.getDataFolder();
        petService.readData(petDataFolder);
    }
    
    /**
     * 固定使用模板petpet
     */
    @SubCommand("摸")
    public void petpet(CommandSender sender, 
            @Name("摸的对象") User target) {
        useTemplateGeneral(sender, "petpet", target, (String[])null);
    }
    
    /**
     * 由用户选择模板petpet
     */
    @SubCommand("选择模板")
    public void useTemplateWithTarget(CommandSender sender, 
            @Name("模板key") String petkey, 
            @Name("模板头像位置替换的对象") User target, 
            @Name("(可选)模板文本替换的参数") String... petReplaceArgs) {
        useTemplateGeneral(sender, petkey, target, petReplaceArgs);
    }
    
    /**
     * 由用户选择模板petpet
     */
    @SubCommand("选择模板")
    public void useTemplateWithoutTarget(CommandSender sender, 
            @Name("模板key") String petkey, 
            @Name("(可选)模板文本替换的参数") String... petReplaceArgs) {
        useTemplateGeneral(sender, petkey, null, petReplaceArgs);
    }
    
    private void useTemplateGeneral(CommandSender sender, String petkey, User target, String... petReplaceArgs) {
        // 准备制图参数
        BufferedImage fromAvatarImage = sender.getUser() != null ? ImageSynthesis.getAvatarImage(sender.getUser().getAvatarUrl()) : null;
        BufferedImage toAvatarImage = ImageSynthesis.getAvatarImage(target.getAvatarUrl());
        TextExtraData textExtraData = petReplaceArgs != null ? new TextExtraData("", "", "", Arrays.asList(petReplaceArgs)) : null;
        // 制图
        Pair<InputStream, String> resultPair = petService.generateImage(fromAvatarImage, toAvatarImage, petkey, textExtraData, null);
        // 使用制图结果
        if (resultPair != null) {
            try (ExternalResource externalResource = ExternalResource.create(resultPair.getFirst())) {
                Image image = sender.getSubject().uploadImage(externalResource);
                sender.sendMessage(image);
            } catch (Exception e) {
                plugin.getLogger().error("使用petService resultPair时异常", e); 
                sender.sendMessage("使用制图结果时异常");
            } finally {
                try {
                    resultPair.getFirst().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            plugin.getLogger().warning("未得到petService resultPair");  
            sender.sendMessage("未得到制图结果");
        }
    }
}
