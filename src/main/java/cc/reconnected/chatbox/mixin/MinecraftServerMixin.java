package cc.reconnected.chatbox.mixin;

import cc.reconnected.chatbox.RccChatbox;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at=@At("TAIL"),method = "<init>")
    private void onServerInit(CallbackInfo ci) {
        RccChatbox.LOGGER.info("Setting serverInstance");
        RccChatbox.serverInstance = (MinecraftServer) (Object) this;
    }
}
