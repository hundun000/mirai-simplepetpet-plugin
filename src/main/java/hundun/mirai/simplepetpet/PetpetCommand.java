package hundun.mirai.simplepetpet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import javax.imageio.ImageIO;

import hundun.miraifleet.framework.helper.repository.SingletonDocumentRepository;
import kotlin.Pair;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.CompositeCommand;
import net.mamoe.mirai.console.command.ConsoleCommandSender;
import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext;
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;
import xmmt.dituon.share.AvatarExtraData;
import xmmt.dituon.share.BasePetService;
import xmmt.dituon.share.BaseServiceConfig;
import xmmt.dituon.share.ImageSynthesis;
import xmmt.dituon.share.TextExtraData;

public class PetpetCommand extends CompositeCommand {
    
    private final BasePetService petService;
    private final JvmPlugin plugin;
    private final SingletonDocumentRepository<BaseServiceConfig> repository;
    private BufferedImage defaultAvatar;
    private File saveFolder;
    
    public PetpetCommand(JvmPlugin plugin) {
        super(plugin, "制图", new String[]{}, "我是PetpetCommand", plugin.getParentPermission(), CommandArgumentContext.EMPTY);
        this.plugin = plugin;
        this.petService = new BasePetService();
        this.repository = new SingletonDocumentRepository<BaseServiceConfig>(
                plugin, 
                plugin.resolveConfigFile("repository.json"), 
                BaseServiceConfig.class,
                () -> new BaseServiceConfig()
                );
        initPetService(plugin);
        initSelf();
    }
    
    /**
     * - 使用SingletonDocumentRepository工具读取config。<br>
     * - 以相对于插件数据目录的方式读取petService所需data目录。
     */
    private void initPetService(JvmPlugin plugin) {
        
        BaseServiceConfig config = repository.findSingleton();

        petService.readBaseServiceConfig(config);
        
        File petDataFolder = plugin.resolveDataFile("templates");
        petService.readData(petDataFolder);
    }
    
    private void initSelf() {
        try {
            defaultAvatar = ImageIO.read(plugin.resolveDataFile("defaultAvatar.png"));
        } catch (IOException e) {
            plugin.getLogger().error(e);
        }
        
        saveFolder = plugin.resolveDataFile("save");
        if (!saveFolder.exists()) {
            saveFolder.mkdirs();
        }
    }
    
    /**
     * 固定使用模板petpet
     */
    @SubCommand("摸")
    public void petpetCommand(CommandSender sender, 
            @Name("摸的对象") User target) {
        useTemplateAndSend(sender, "petpet", target, (String[])null);
    }
    
    /**
     * 仅限Console触发，使用defaultAvatar制图，并保存结果到本地
     */
    @SubCommand("测试模板")
    public void useTemplateAndSaveCommand(ConsoleCommandSender sender, 
            @Name("模板key") String petkey, 
            @Name("(可选)模板文本替换的参数") String... petReplaceArgs) {
        useTemplateAndSave(sender, petkey, null, petReplaceArgs);
    }
    
    /**
     * 由用户选择模板
     */
    @SubCommand("选择模板")
    public void useTemplateWithTargetCommand(CommandSender sender, 
            @Name("模板key") String petkey, 
            @Name("模板头像位置替换的对象") User target, 
            @Name("(可选)模板文本替换的参数") String... petReplaceArgs) {
        useTemplateAndSend(sender, petkey, target, petReplaceArgs);
    }
    
    /**
     * 由用户选择模板
     */
    @SubCommand("选择模板")
    public void useTemplateWithoutTargetCommand(CommandSender sender, 
            @Name("模板key") String petkey, 
            @Name("(可选)模板文本替换的参数") String... petReplaceArgs) {
        useTemplateAndSend(sender, petkey, null, petReplaceArgs);
    }
    
    private void useTemplateAndSave(CommandSender sender, String petkey, User target, String... petReplaceArgs) {
        BufferedImage fromAvatarImage = defaultAvatar;
        BufferedImage toAvatarImage = defaultAvatar;
        
        Pair<InputStream, String> resultPair = useTemplate(fromAvatarImage, toAvatarImage, petkey, petReplaceArgs);  
        // 使用制图结果
        if (resultPair != null) {
            String saveName = petkey + "-" + System.currentTimeMillis() + "." + resultPair.getSecond();
            File saveFile = plugin.resolveDataFile(saveFolder.getAbsolutePath() + File.separator + saveName);
            try {
                copyInputStreamToFile(resultPair.getFirst(), saveFile);
                sender.sendMessage("保存成功：" + saveName);
            } catch (IOException e) {
                plugin.getLogger().error(e);
                sender.sendMessage("保存失败");
            }
        } else {
            plugin.getLogger().warning("未得到petService resultPair");  
            sender.sendMessage("未得到制图结果");
        }
    }
    
    private void useTemplateAndSend(CommandSender sender, String petkey, User target, String... petReplaceArgs) {
        BufferedImage fromAvatarImage = sender.getUser() != null ? ImageSynthesis.getAvatarImage(sender.getUser().getAvatarUrl()) : null;
        BufferedImage toAvatarImage = ImageSynthesis.getAvatarImage(target.getAvatarUrl());
        
        Pair<InputStream, String> resultPair = useTemplate(fromAvatarImage, toAvatarImage, petkey, petReplaceArgs);
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
    
    private Pair<InputStream, String> useTemplate(BufferedImage fromAvatarImage, BufferedImage toAvatarImage, String petkey, String... petReplaceArgs) {
        // 准备制图参数
        TextExtraData textExtraData = petReplaceArgs != null ? new TextExtraData("", "", "", Arrays.asList(petReplaceArgs)) : null;
        AvatarExtraData avatarExtraData = new AvatarExtraData(fromAvatarImage, toAvatarImage, null, null);
        // 制图
        Pair<InputStream, String> resultPair = petService.generateImage(petkey, avatarExtraData, textExtraData, null);
        return resultPair;
    }
    
    private void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {
        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[8192];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
