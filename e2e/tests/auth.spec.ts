import { expect, test } from "@playwright/test";
import { logIn } from "../support/ui";
import { createVerifiedUser, newUser } from "../support/users";
import { waitForVerificationToken } from "../support/mailpit";

test.describe("authentication", () => {
  test("a new user registers, verifies the email and logs in", async ({
    page,
  }) => {
    const user = newUser("register");

    await test.step("register", async () => {
      await page.goto("/register");
      await page.getByLabel("Name").fill(user.name);
      await page.getByLabel("Email").fill(user.email);
      await page.getByLabel("Password", { exact: true }).fill(user.password);
      await page.getByLabel("Confirm password").fill(user.password);
      await page.getByRole("button", { name: "Register" }).click();
      await expect(
        page.getByText("we have sent a verification email"),
      ).toBeVisible();
    });

    await test.step("cannot log in before verifying the email", async () => {
      await page.goto("/login");
      await page.getByLabel("Email").fill(user.email);
      await page.getByLabel("Password").fill(user.password);
      await page.getByRole("button", { name: "Log in" }).click();
      await expect(
        page.getByText("Your email address is not verified yet"),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Resend verification email" }),
      ).toBeVisible();
    });

    await test.step("verify through the emailed link", async () => {
      const token = await waitForVerificationToken(user.email);
      await page.goto(`/verify-email?token=${token}`);
      await expect(
        page.getByText("Your email has been verified"),
      ).toBeVisible();
    });

    await test.step("log in", async () => {
      await logIn(page, user);
      await expect(page.getByText(user.name)).toBeVisible();
    });
  });

  test("a wrong password is rejected", async ({ page }) => {
    const user = await createVerifiedUser("wrongpass");

    await page.goto("/login");
    await page.getByLabel("Email").fill(user.email);
    await page.getByLabel("Password").fill("not-the-password");
    await page.getByRole("button", { name: "Log in" }).click();

    await expect(page.getByText("Invalid email or password")).toBeVisible();
  });

  test("an invalid verification link is rejected", async ({ page }) => {
    await page.goto("/verify-email?token=this-token-does-not-exist");

    await expect(
      page.getByText("The link is invalid or has expired"),
    ).toBeVisible();
  });

  test("logging out protects the pages again", async ({ page }) => {
    const user = await createVerifiedUser("logout");
    await logIn(page, user);

    await page.getByRole("button", { name: "Log out" }).click();
    await page.goto("/incidents");

    await expect(page).toHaveURL(/\/login/);
  });
});
