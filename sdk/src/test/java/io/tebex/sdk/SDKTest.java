package io.tebex.sdk;

import io.tebex.sdk.obj.*;
import io.tebex.sdk.obj.Package;
import io.tebex.sdk.platform.MockPlatform;
import io.tebex.sdk.triage.EnumEventLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SDKTest {
    SDK sdk;
    MockPlatform platform;

    String secret = ""; //TODO replace with your secret key
    String testUsername = ""; //TODO replace with a username to test against
    String testUuid = ""; //TODO replace with a uuid to test against
    @BeforeEach
    void setUp() {
        platform = new MockPlatform();
        sdk = new SDK(platform, secret);
    }

    @Test
    void getServerInformation() {
        sdk.getServerInformation().thenAccept(info -> {
            assertNotNull(info);
            assertNotNull(info.getServer());
            assertNotNull(info.getStore());
            assertNotNull(info.getServer().getName());
            assertNotEquals(info.getServer().getId(), 0);
            assertNotNull(info.getStore().getDomain());
            assertNotNull(info.getStore().getName());
            assertNotEquals(info.getStore().getId(), 0);
            assertNotNull(info.getStore().getCurrency());
            assertNotNull(info.getStore().getCurrency().getSymbol());
            assertNotNull(info.getStore().getCurrency().getIso4217());
        }).exceptionally(throwable -> {
            fail(throwable.getMessage());
            return null;
        }).join();
    }

    @Test
    void getDuePlayers() {
        sdk.getDuePlayers().thenAccept(players -> {
            assertNotNull(players);
            assertNotNull(players.getPlayers());
            assertNotEquals(players.getNextCheck(), 0);
        }).exceptionally(throwable -> {
            fail(throwable.getMessage());
            return null;
        }).join();
    }

    @Test
    void getOfflineCommands() {
        sdk.getOfflineCommands().thenAccept(offlineCmds -> {
            assertNotNull(offlineCmds.getCommands());
        }).exceptionally(throwable -> {
            fail(throwable.getMessage());
            return null;
        }).join();
    }

    @Test
    void getOnlineCommands() {
        sdk.getDuePlayers().thenAccept(players -> {
            assertNotNull(players);
            assertNotNull(players.getPlayers());
            assertNotEquals(players.getNextCheck(), 0);
            for (int i = 0; i < players.getPlayers().size(); i++) {
                QueuedPlayer player = players.getPlayers().get(i);
                sdk.getOnlineCommands(player).thenAccept(onlineCmds -> {
                    assertNotNull(onlineCmds);

                    for (QueuedCommand command : onlineCmds) {
                        assertNotNull(command);
                        assertNotNull(command.getCommand());
                        assertNotNull(command.getParsedCommand());
                        assertNotEquals(command.getId(), 0);
                        assertNotEquals(command.getPayment(), 0);
                        assertNotEquals(command.getDelay(), 0);
                        assertNotEquals(command.getPackageId(), 0);
                        assertNotNull(command.getPlayer());
                    }

                }).exceptionally(throwable -> {
                    fail(throwable.getMessage());
                    return null;
                }).join();
            }
        }).exceptionally(throwable -> {
            fail(throwable.getMessage());
            return null;
        }).join();
    }

    @Test
    void deleteCommands() {
        sdk.deleteCommands(Arrays.asList(1, 2, 3)).thenAccept(result -> {
            assertTrue(result);
        }).exceptionally(throwable -> {
            fail(throwable.getMessage());
            return null;
        }).join();
    }

    @Test
    void getCommunityGoals() {
        sdk.getCommunityGoals().thenAccept(goals -> {
            assertNotNull(goals);
            for (CommunityGoal goal : goals) {
                assertNotNull(goal.getName());
                assertNotNull(goal.getDescription());
                assertNotEquals(goal.getId(), 0);
            }
        }).join();
    }

    @Test
    void getCommunityGoal() {
        sdk.getCommunityGoals().thenAccept(goals -> {
            if (!goals.isEmpty()) {
                sdk.getCommunityGoal(goals.get(0).getId()).thenAccept(goal -> {
                    assertNotNull(goal);
                    assertNotNull(goal.getName());
                    assertNotNull(goal.getDescription());
                    assertNotEquals(goal.getId(), 0);
                }).exceptionally(throwable -> {
                    fail(throwable.getMessage());
                    return null;
                }).join();
            }
        }).join();
    }

    @Test
    void createCheckoutUrl() {
        sdk.getPackages().thenAccept(packages -> {
            if (!packages.isEmpty()) {
                sdk.createCheckoutUrl(packages.get(0).getId(), testUsername).thenAccept(url -> {
                    assertNotNull(url);
                    assertFalse(url.getUrl().isEmpty());
                }).join();
            }
        }).join();
    }

    @Test
    void getCoupons() {
        sdk.getCoupons().thenAccept(coupons -> {
            assertNotNull(coupons);
            for (Coupon coupon : coupons.getData()) {
                assertTrue(coupon.getId() > 0);
                assertFalse(coupon.getCode().isEmpty());
                assertNotNull(coupon.getDiscount());
                assertNotNull(coupon.getBasketType());
                assertNotNull(coupon.getDiscount());
                assertNotNull(coupon.getEffective());
                assertNotNull(coupon.getStartDate());
            }
        }).join();
    }

    @Test
    void getCoupon() {
        sdk.getCoupons().thenAccept(coupons -> {
            for (Coupon c : coupons.getData()) {
                sdk.getCoupon(c.getId()).thenAccept(coupon -> {
                    assertTrue(coupon.getId() > 0);
                    assertFalse(coupon.getCode().isEmpty());
                    assertNotNull(coupon.getDiscount());
                    assertNotNull(coupon.getBasketType());
                    assertNotNull(coupon.getDiscount());
                    assertNotNull(coupon.getEffective());
                    assertNotNull(coupon.getStartDate());
                }).join();
            }
        }).join();
    }

    @Test
    void getListing() {
        sdk.getListing().thenAccept(listings -> {
            for (Category category : listings) {
                assertFalse(category.getGuiItem().isEmpty());
                assertTrue(category.getId() > 0);
                assertFalse(category.getName().isEmpty());
            }
        }).join();
    }

    @Test
    void getPackage() {
        sdk.getPackages().thenAccept(packages -> {
            for (Package p : packages) {
                sdk.getPackage(p.getId()).thenAccept(pkg -> {
                    assertNotNull(pkg);
                    assertEquals(p.getName(), pkg.getName());
                    assertEquals(p.getId(), pkg.getId());
                    assertFalse(p.getItemId().isEmpty());
                }).join();
            }
        }).join();
    }

    @Test
    void getPackages() {
        sdk.getPackages().thenAccept(packages -> {
            for (Package p : packages) {
                assertTrue(p.getId() > 0);
                assertFalse(p.getName().isEmpty());
                assertNotNull(p.getCategory());
            }
        }).join();
    }

    @Test
    void getSecretKey() {
        assertEquals(sdk.getSecretKey(), "");
        sdk.setSecretKey(secret);
        assertEquals(sdk.getSecretKey(), secret);
    }

    @Test
    void setSecretKey() {
        sdk.setSecretKey(secret);
        assertEquals(sdk.getSecretKey(), secret);
    }

    @Test
    void sendPluginEvents() {
        platform.createPluginEvent(EnumEventLevel.INFO, "Test");
        sdk.sendPluginEvents().thenAccept(success -> {
            if (!success) {
                fail("Failed to send plugin events");
            }
        }).join();
        assertTrue(platform.getPluginEvents().isEmpty());
    }

    @Test
    void sendJoinEvents() {
        platform.createJoinEvent(testUuid, testUsername, "127.0.0.1");
        sdk.sendJoinEvents(platform.getJoinEvents()).thenAccept(success -> {
            if (!success) {
                fail("Failed to send join events");
            }
        }).join();
        platform.getJoinEvents().clear();
    }
}