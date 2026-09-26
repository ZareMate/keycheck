package com.zaremate.airport_security.mixin;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(SignBlockEntity.class)
public interface SignBlockEntityAccessor {
    @Accessor("frontText")
    void airport_security$setFrontText(SignText text);

    @Accessor("backText")
    void airport_security$setBackText(SignText text);

    @Accessor("playerWhoMayEdit")
    void airport_security$setEditor(UUID editor);
}
