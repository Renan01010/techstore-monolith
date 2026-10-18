package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PromotionsMarketingSchemaIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String COUPON = "insert into promotions.coupons (code, benefit_type, benefit_value, currency, valid_from, valid_until, max_total_uses, max_uses_per_user) "
            + "values (?, ?, ?, ?, now(), now() + interval '30 days', ?, ?) returning id";

    private static UUID coupon() {
        return insert(COUPON, unique("CODE").toUpperCase(), "fixed_amount", 10, "BRL", null, null);
    }

    @Test
    void couponCodeIsUniqueAndNormalized() {
        String code = unique("PROMO").toUpperCase();
        insert(COUPON, code, "percentage", 10, null, 100, 1);
        assertSqlState(UNIQUE, () -> insert(COUPON, code, "percentage", 10, null, null, null));
        assertSqlState(CHECK, () -> insert(COUPON, code.toLowerCase(), "percentage", 10, null, null, null));
    }

    @Test
    void couponBenefitRulesAreEnforced() {
        insert(COUPON, unique("A").toUpperCase(), "percentage", 100, null, null, null);
        insert(COUPON, unique("B").toUpperCase(), "percentage", 0, null, null, null);
        assertSqlState(CHECK, () -> insert(COUPON, unique("C").toUpperCase(), "percentage", 100.01, null, null, null));
        assertSqlState(CHECK, () -> insert(COUPON, unique("D").toUpperCase(), "fixed_amount", -1, "BRL", null, null));
        assertSqlState(CHECK, () -> insert(COUPON, unique("E").toUpperCase(), "bogo", 1, null, null, null));
        assertSqlState(CHECK, () -> insert(COUPON, unique("F").toUpperCase(), "fixed_amount", 5, null, null, null));
    }

    @Test
    void couponValidityAndLimitsAreEnforced() {
        assertSqlState(CHECK, () -> update("insert into promotions.coupons (code, benefit_type, benefit_value, valid_from, valid_until) "
                + "values (?, 'percentage', 5, now(), now())", unique("V").toUpperCase()));
        assertSqlState(CHECK, () -> insert(COUPON, unique("G").toUpperCase(), "percentage", 5, null, 0, null));
        assertSqlState(CHECK, () -> insert(COUPON, unique("H").toUpperCase(), "percentage", 5, null, null, -1));
        insert(COUPON, unique("I").toUpperCase(), "percentage", 5, null, 10, 3);
    }

    @Test
    void usageAllowsRepeatsAndIsPreservedHistory() {
        UUID coupon = coupon();
        UUID user = user();
        String insert = "insert into promotions.coupon_usage (coupon_id, user_id) values (?, ?)";
        update(insert, coupon, user);
        update(insert, coupon, user);
        assertSqlState(FOREIGN_KEY, () -> update(insert, UUID.randomUUID(), user));
        assertSqlState(FOREIGN_KEY, () -> update(insert, coupon, UUID.randomUUID()));

        assertSqlState(FOREIGN_KEY, () -> update("delete from promotions.coupons where id = ?", coupon));
        update("delete from identity.users where id = ?", user);
        assertThat(count("select count(*) from promotions.coupon_usage where coupon_id = ? and user_id is null", coupon)).isEqualTo(2);
    }

    @Test
    void newsletterEmailIsUniqueNormalizedAndStateValidated() {
        String email = unique("n") + "@example.com";
        update("insert into marketing.newsletter_subscriptions (email) values (?)", email);
        assertSqlState(UNIQUE, () -> update("insert into marketing.newsletter_subscriptions (email) values (?)", email));
        assertSqlState(CHECK, () -> update("insert into marketing.newsletter_subscriptions (email) values (?)", email.toUpperCase()));
        for (String state : new String[] {"pending_confirmation", "subscribed", "unsubscribed"}) {
            update("insert into marketing.newsletter_subscriptions (email, subscription_state) values (?, ?)", unique("s") + "@example.com", state);
        }
        assertSqlState(CHECK, () -> update("insert into marketing.newsletter_subscriptions (email, subscription_state) values (?, 'gone')",
                unique("x") + "@example.com"));
    }

    @Test
    void campaignAndRecipientStatesAreValidated() {
        for (String state : new String[] {"draft", "scheduled", "completed", "cancelled"}) {
            update("insert into marketing.marketing_emails (campaign_name, subject, campaign_state) values ('C', 'S', ?)", state);
        }
        assertSqlState(CHECK, () -> update("insert into marketing.marketing_emails (campaign_name, subject, campaign_state) values ('C', 'S', 'sent')"));
        UUID campaign = campaign();
        for (String state : new String[] {"pending", "sent", "delivered", "bounced", "failed", "unsubscribed"}) {
            update("insert into marketing.email_recipients (marketing_email_id, recipient_email, delivery_state) values (?, ?, ?)",
                    campaign, unique("r") + "@example.com", state);
        }
        assertSqlState(CHECK, () -> update("insert into marketing.email_recipients (marketing_email_id, recipient_email, delivery_state) values (?, ?, 'opened')",
                campaign, unique("r") + "@example.com"));
    }

    @Test
    void recipientEmailIsUniquePerCampaignOnly() {
        UUID first = campaign();
        UUID second = campaign();
        String email = unique("r") + "@example.com";
        String insert = "insert into marketing.email_recipients (marketing_email_id, recipient_email) values (?, ?)";
        update(insert, first, email);
        assertSqlState(UNIQUE, () -> update(insert, first, email));
        update(insert, second, email);
        assertSqlState(FOREIGN_KEY, () -> update(insert, UUID.randomUUID(), email));
    }

    @Test
    void campaignWithRecipientsIsProtectedAndUserRemovalKeepsTheSnapshot() {
        UUID campaign = campaign();
        UUID user = user();
        UUID recipient = insert("insert into marketing.email_recipients (marketing_email_id, user_id, recipient_email) values (?, ?, 'keep@example.com') returning id",
                campaign, user);
        assertSqlState(FOREIGN_KEY, () -> update("delete from marketing.marketing_emails where id = ?", campaign));
        update("delete from identity.users where id = ?", user);
        assertThat(scalar("select user_id from marketing.email_recipients where id = ?", recipient)).isNull();
        assertThat(scalar("select recipient_email from marketing.email_recipients where id = ?", recipient)).isEqualTo("keep@example.com");
    }
}
