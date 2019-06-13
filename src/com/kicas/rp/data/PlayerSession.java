package com.kicas.rp.data;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.util.Decoder;
import com.kicas.rp.util.Encoder;
import com.kicas.rp.util.Serializable;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;
import java.util.UUID;

/**
 * Stores transient and persistent data for a given player.
 */
public class PlayerSession implements Serializable {
    private UUID uuid;
    private int claimBlocks;
    // All of the following can be null
    private RegionHighlighter currentHighlighter;
    private PlayerRegionAction action;
    private Region currentSelectedRegion;
    private Location lastClickedBlock;

    public PlayerSession(UUID uuid) {
        this.uuid = uuid;
        this.claimBlocks = 0;
        this.currentHighlighter = null;
        this.action = null;
        this.currentSelectedRegion = null;
        this.lastClickedBlock = null;
    }

    public PlayerSession() {
        this(null);
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getClaimBlocks() {
        return claimBlocks;
    }

    public void addClaimBlocks(int amount) {
        claimBlocks += amount;
    }

    public void subtractClaimBlocks(int amount) {
        claimBlocks -= amount;
    }

    /**
     * Removes the current highlighter and replaces it with the new, given highlighter. If the given highlighter is not
     * null, then its blocks are shown to the player.
     * @param highlighter the region highlighter.
     */
    public void setRegionHighlighter(RegionHighlighter highlighter) {
        if(currentHighlighter != null && !currentHighlighter.isComplete())
            currentHighlighter.remove();
        currentHighlighter = highlighter;
        if(highlighter != null)
            Bukkit.getScheduler().runTaskLater(RegionProtection.getInstance(), currentHighlighter::showBlocks, 1L);
    }

    public PlayerRegionAction getAction() {
        return action;
    }

    public void setAction(PlayerRegionAction action) {
        this.action = action;
    }

    public Region getCurrentSelectedRegion() {
        return currentSelectedRegion;
    }

    public void setCurrentSelectedRegion(Region currentSelectedRegion) {
        this.currentSelectedRegion = currentSelectedRegion;
    }

    public Location getLastClickedBlock() {
        return lastClickedBlock;
    }

    public void setLastClickedBlock(Location lastClickedBlock) {
        this.lastClickedBlock = lastClickedBlock;
    }

    @Override
    public void serialize(Encoder encoder) throws IOException {
        encoder.writeUuid(uuid);
        encoder.writeInt(claimBlocks);
    }

    @Override
    public void deserialize(Decoder decoder) throws IOException {
        uuid = decoder.readUuid();
        claimBlocks = decoder.readInt();
    }
}