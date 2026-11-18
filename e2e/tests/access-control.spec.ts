import { randomUUID } from "node:crypto";
import { expect, test } from "@playwright/test";
import { createIncident, login } from "../support/api";
import { logIn } from "../support/ui";
import { createAgent, createVerifiedUser } from "../support/users";

test.describe("access control", () => {
  test("a user cannot see somebody else's incident", async ({ page }) => {
    const owner = await createVerifiedUser("owner");
    const other = await createVerifiedUser("other");
    const incidentId = await createIncident(
      await login(owner.email, owner.password),
      `Private ${randomUUID().slice(0, 8)}`,
    );

    await logIn(page, other);
    await expect(page.getByText("No incidents found")).toBeVisible();

    await page.goto(`/incidents/${incidentId}`);
    await expect(page.getByText("Incident not found")).toBeVisible();
  });

  test("regular users do not get the statistics", async ({ page }) => {
    const user = await createVerifiedUser("nostats");
    await logIn(page, user);

    await expect(page.getByRole("link", { name: "Statistics" })).toHaveCount(0);

    await page.goto("/stats");
    await expect(page).toHaveURL(/\/incidents$/);
  });

  test("agents can open the statistics and change the period", async ({
    page,
  }) => {
    const agent = await createAgent("stats");
    await logIn(page, agent);

    await page.getByRole("link", { name: "Statistics" }).click();
    await expect(
      page.getByRole("heading", { name: "Statistics", exact: true }),
    ).toBeVisible();
    await expect(page.getByText("Total incidents")).toBeVisible();

    await page.getByLabel("Period").selectOption("7");
    await expect(page.getByText("Created and resolved per day")).toBeVisible();
  });
});
