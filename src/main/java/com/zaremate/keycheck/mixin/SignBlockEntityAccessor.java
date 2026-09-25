package com.zaremate.keycheck.mixin;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(SignBlockEntity.class)
public interface SignBlockEntityAccessor {
    @Accessor("frontText")
    void keycheck$setFrontText(SignText text);

    @Accessor("backText")
    void keycheck$setBackText(SignText text);

    @Accessor("playerWhoMayEdit")
    void keycheck$setEditor(UUID editor);
}
