package net.countercraft.movecraft.warfare.events;

import org.jetbrains.annotations.NotNull;
import org.bukkit.event.Event;
import net.countercraft.movecraft.warfare.assault.Assault;


public abstract class AssaultEvent extends Event {
    @NotNull protected final Assault assault;

    public AssaultEvent(@NotNull Assault assault) {
        this.assault = assault;
    }

    @NotNull
    public final Assault getAssault() {
        return assault;
    }
}