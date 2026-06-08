package dev.maire.thinair.capability;

import dev.maire.thinair.api.AirQualityLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AirBubblePositionsCapability implements INBTSerializable<CompoundTag> {
    public static final String TAG_POSITIONS = "Positions";
    public static final String TAG_QUALITY = "AirQuality";
    public static final String TAG_SKIP_COUNT_LEFT = "SkipCountLeft";

    private int skipCountLeft;
    private Map<BlockPos, AirQualityLevel> airBubbleEntries = new LinkedHashMap<>();

    public Map<BlockPos, AirQualityLevel> getAirBubblePositionsView() {
        return Collections.unmodifiableMap(this.airBubbleEntries);
    }

    public Map<BlockPos, AirQualityLevel> getAirBubblePositions() {
        return this.airBubbleEntries;
    }

    public int getSkipCountLeft() {
        return this.skipCountLeft;
    }

    public void setSkipCountLeft(int skipCountLeft) {
        this.skipCountLeft = skipCountLeft;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        ListTag positions = new ListTag();
        byte[] qualitiesArr = new byte[this.airBubbleEntries.size()];
        int i = 0;
        for (Map.Entry<BlockPos, AirQualityLevel> entry : this.airBubbleEntries.entrySet()) {
            positions.add(NbtUtils.writeBlockPos(entry.getKey()));
            qualitiesArr[i] = (byte) entry.getValue().ordinal();
            i++;
        }
        tag.put(TAG_POSITIONS, positions);
        tag.put(TAG_QUALITY, new ByteArrayTag(qualitiesArr));
        tag.putInt(TAG_SKIP_COUNT_LEFT, this.skipCountLeft);
        return tag;
    }

    public void read(CompoundTag tag) {
        ListTag positions = tag.getList(TAG_POSITIONS, Tag.TAG_INT_ARRAY);
        byte[] qualities = tag.getByteArray(TAG_QUALITY);
        Map<BlockPos, AirQualityLevel> entries = new LinkedHashMap<>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            int[] coords = ((IntArrayTag) positions.get(i)).getAsIntArray();
            entries.put(new BlockPos(coords[0], coords[1], coords[2]), AirQualityLevel.values()[qualities[i]]);
        }
        this.airBubbleEntries = entries;
        this.skipCountLeft = tag.getInt(TAG_SKIP_COUNT_LEFT);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return this.write();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.read(tag);
    }
}
