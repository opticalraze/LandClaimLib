package com.opticalraze.landclaimlib.Events;

import com.opticalraze.landclaimlib.Claim;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link org.bukkit.event.Event Event} called when a {@link Claim} is resized.
 */
public class ClaimResizeEvent extends ClaimModifiedEvent
{

    /**
     * Construct a new {@code ClaimResizeEvent}.
     *
     * <p>The actor causing modification may not be present if done by plugins.
     *
     * @param from the unmodified {@link Claim}
     * @param to the resulting {@code Claim}
     * @param modifier the {@link CommandSender} causing modification
     */
    public ClaimResizeEvent(@NotNull Claim from, @NotNull Claim to, @Nullable CommandSender modifier)
    {
        super(from, to, modifier);
    }

}
