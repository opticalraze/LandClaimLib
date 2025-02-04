/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.opticalraze.landclaimlib;

import com.opticalraze.landclaimlib.Events.ClaimExpirationEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

class CleanupUnusedClaimTask implements Runnable
{
    Claim claim;
    PlayerData ownerData;
    OfflinePlayer ownerInfo;

    CleanupUnusedClaimTask(Claim claim, PlayerData ownerData, OfflinePlayer ownerInfo)
    {
        this.claim = claim;
        this.ownerData = ownerData;
        this.ownerInfo = ownerInfo;
    }

    @Override
    public void run()
    {


        //determine area of the default chest claim
        int areaOfDefaultClaim = 0;
        if (LandClaimLib.instance.config_claims_automaticClaimsForNewPlayersRadius >= 0)
        {
            areaOfDefaultClaim = (int) Math.pow(LandClaimLib.instance.config_claims_automaticClaimsForNewPlayersRadius * 2 + 1, 2);
        }

        //if this claim is a chest claim and those are set to expire
        if (ownerData.getClaims().size() == 1 && claim.getArea() <= areaOfDefaultClaim && LandClaimLib.instance.config_claims_chestClaimExpirationDays > 0)
        {
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
            Calendar sevenDaysAgo = Calendar.getInstance();
            sevenDaysAgo.add(Calendar.DATE, -LandClaimLib.instance.config_claims_chestClaimExpirationDays);
            if (sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed())))
            {
                if (expireEventCanceled())
                    return;
                claim.removeSurfaceFluids(null);
                LandClaimLib.instance.dataStore.deleteClaim(claim, true, true);

                //if configured to do so, restore the land to natural
                if (LandClaimLib.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) || LandClaimLib.instance.config_claims_survivalAutoNatureRestoration)
                {
                    LandClaimLib.instance.restoreClaim(claim, 0);
                }

                LandClaimLib.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.", CustomLogEntryTypes.AdminActivity);
            }
        }

        //if configured to always remove claims after some inactivity period without exceptions...
        else if (LandClaimLib.instance.config_claims_expirationDays > 0)
        {
            Calendar earliestPermissibleLastLogin = Calendar.getInstance();
            earliestPermissibleLastLogin.add(Calendar.DATE, -LandClaimLib.instance.config_claims_expirationDays);

            if (earliestPermissibleLastLogin.getTime().after(new Date(ownerInfo.getLastPlayed())))
            {
                if (expireEventCanceled())
                    return;
                //make a copy of this player's claim list
                Vector<Claim> claims = new Vector<>(ownerData.getClaims());

                //delete them
                LandClaimLib.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
                LandClaimLib.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.", CustomLogEntryTypes.AdminActivity);
                LandClaimLib.AddLogEntry("earliestPermissibleLastLogin#getTime: " + earliestPermissibleLastLogin.getTime(), CustomLogEntryTypes.Debug, true);
                LandClaimLib.AddLogEntry("ownerInfo#getLastPlayed: " + ownerInfo.getLastPlayed(), CustomLogEntryTypes.Debug, true);

                for (Claim claim : claims)
                {
                    //if configured to do so, restore the land to natural
                    if (LandClaimLib.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) || LandClaimLib.instance.config_claims_survivalAutoNatureRestoration)
                    {
                        LandClaimLib.instance.restoreClaim(claim, 0);
                    }
                }
            }
        }
        else if (LandClaimLib.instance.config_claims_unusedClaimExpirationDays > 0 && LandClaimLib.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
        {
            //avoid scanning large claims and administrative claims
            if (claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;

            //otherwise scan the claim content
            int minInvestment = 400;

            long investmentScore = claim.getPlayerInvestmentScore();

            if (investmentScore < minInvestment)
            {
                //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
                Calendar sevenDaysAgo = Calendar.getInstance();
                sevenDaysAgo.add(Calendar.DATE, -LandClaimLib.instance.config_claims_unusedClaimExpirationDays);
                boolean claimExpired = sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed()));
                if (claimExpired)
                {
                    if (expireEventCanceled())
                        return;
                    LandClaimLib.instance.dataStore.deleteClaim(claim, true, true);
                    LandClaimLib.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + LandClaimLib.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);

                    //restore the claim area to natural state
                    LandClaimLib.instance.restoreClaim(claim, 0);
                }
            }
        }
    }

    public boolean expireEventCanceled()
    {
        //see if any other plugins don't want this claim deleted
        ClaimExpirationEvent event = new ClaimExpirationEvent(this.claim);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
