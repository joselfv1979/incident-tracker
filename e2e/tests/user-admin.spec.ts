import { expect, test } from "@playwright/test";
import { ADMIN_EMAIL, ADMIN_PASSWORD, FRONTEND_URL } from "../support/env";
import { logIn } from "../support/ui";
import { createVerifiedUser } from "../support/users";

test("an administrator promotes a user to agent", async ({ browser }) => {
  const user = await createVerifiedUser("promote");

  const adminContext = await browser.newContext({ baseURL: FRONTEND_URL });
  const userContext = await browser.newContext({ baseURL: FRONTEND_URL });
  const adminPage = await adminContext.newPage();
  const userPage = await userContext.newPage();

  try {
    await test.step("the user has no access to the statistics yet", async () => {
      await logIn(userPage, user);
      await expect(
        userPage.getByRole("link", { name: "Statistics" }),
      ).toHaveCount(0);
    });

    await test.step("the administrator opens the user list", async () => {
      await logIn(adminPage, {
        name: "Administrator",
        email: ADMIN_EMAIL,
        password: ADMIN_PASSWORD,
      });
      await adminPage.getByRole("link", { name: "Users" }).click();
      await expect(
        adminPage.getByRole("heading", { name: "Users", exact: true }),
      ).toBeVisible();
      await expect(
        adminPage.getByLabel("Role of Administrator", { exact: true }),
      ).toBeDisabled();
    });

    await test.step("the administrator changes the role and confirms it", async () => {
      await adminPage.getByLabel(`Role of ${user.name}`).selectOption("AGENT");
      await expect(
        adminPage.getByText(
          `Change the role of ${user.name} from User to Agent`,
        ),
      ).toBeVisible();
      await adminPage.getByRole("button", { name: "Confirm" }).click();
      await expect(adminPage.getByLabel(`Role of ${user.name}`)).toHaveValue(
        "AGENT",
      );
    });

    await test.step("the change applies right away, without logging in again", async () => {
      await userPage.reload();
      await expect(
        userPage.getByRole("link", { name: "Statistics" }),
      ).toBeVisible();
    });
  } finally {
    await adminContext.close();
    await userContext.close();
  }
});

test("regular users cannot open the user administration", async ({ page }) => {
  const user = await createVerifiedUser("noadmin");
  await logIn(page, user);

  await expect(page.getByRole("link", { name: "Users" })).toHaveCount(0);

  await page.goto("/admin/users");
  await expect(page).toHaveURL(/\/incidents$/);
});
