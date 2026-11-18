import { randomUUID } from "node:crypto";
import { expect, test } from "@playwright/test";
import { FRONTEND_URL } from "../support/env";
import { logIn } from "../support/ui";
import { createAgent, createVerifiedUser } from "../support/users";

test("a reporter and an agent resolve an incident together", async ({
  browser,
}) => {
  const reporter = await createVerifiedUser("reporter");
  const agent = await createAgent("agent");

  const id = randomUUID().slice(0, 8);
  const title = `Printer on fire ${id}`;
  const internalNote = `Probably the fuser ${id}`;

  // Two separate browsers, as two different people would use it
  const reporterContext = await browser.newContext({ baseURL: FRONTEND_URL });
  const agentContext = await browser.newContext({ baseURL: FRONTEND_URL });
  const reporterPage = await reporterContext.newPage();
  const agentPage = await agentContext.newPage();

  try {
    await test.step("the reporter reports an incident", async () => {
      await logIn(reporterPage, reporter);
      await reporterPage.getByRole("link", { name: "New incident" }).click();
      await reporterPage.getByLabel("Title").fill(title);
      await reporterPage
        .getByLabel("Description")
        .fill("Smoke is coming out of the printer");
      await reporterPage.getByLabel("Category (optional)").fill("Hardware");
      await reporterPage.getByRole("button", { name: "Create" }).click();

      await expect(
        reporterPage.getByRole("heading", { name: title }),
      ).toBeVisible();
      await expect(
        reporterPage.getByText("created the incident"),
      ).toBeVisible();
    });

    await test.step("the reporter comments and edits the comment", async () => {
      await reporterPage.getByLabel("Add a comment").fill("It happened twice");
      await reporterPage
        .getByRole("button", { name: "Comment", exact: true })
        .click();
      await expect(reporterPage.getByText("It happened twice")).toBeVisible();

      await reporterPage
        .getByRole("button", { name: "Edit", exact: true })
        .click();
      await reporterPage
        .getByLabel("Edit comment")
        .fill("It happened three times");
      await reporterPage.getByRole("button", { name: "Save" }).click();
      await expect(
        reporterPage.getByText("It happened three times"),
      ).toBeVisible();
      await expect(reporterPage.getByText("(edited)")).toBeVisible();
    });

    await test.step("the agent finds the incident", async () => {
      await logIn(agentPage, agent);
      await agentPage.getByLabel("Search").fill(title);
      await agentPage.getByRole("button", { name: "Filter" }).click();
      await agentPage.getByRole("link", { name: title }).click();
      await expect(
        agentPage.getByRole("heading", { name: title }),
      ).toBeVisible();
    });

    await test.step("the agent takes it, assigns it and writes an internal note", async () => {
      await agentPage
        .getByRole("button", { name: "Move to In progress" })
        .click();
      await expect(
        agentPage.getByText("changed the status from Open to In progress"),
      ).toBeVisible();

      await agentPage
        .getByLabel("Assignee")
        .selectOption({ label: agent.name });
      await expect(
        agentPage.getByText(`assigned the incident to ${agent.name}`),
      ).toBeVisible();

      await agentPage
        .getByLabel("Add a comment")
        .fill("We are looking into it");
      await agentPage
        .getByRole("button", { name: "Comment", exact: true })
        .click();
      await expect(agentPage.getByText("We are looking into it")).toBeVisible();

      await agentPage.getByLabel("Add a comment").fill(internalNote);
      await agentPage.getByLabel("Internal note").check();
      await agentPage
        .getByRole("button", { name: "Comment", exact: true })
        .click();
      await expect(agentPage.getByText(internalNote)).toBeVisible();
    });

    await test.step("the reporter sees the progress but not the internal note", async () => {
      await reporterPage.reload();
      await expect(
        reporterPage.getByText("We are looking into it"),
      ).toBeVisible();
      await expect(
        reporterPage.getByText("In progress", { exact: true }),
      ).toBeVisible();
      await expect(reporterPage.getByText(internalNote)).toHaveCount(0);
    });

    await test.step("the agent resolves it", async () => {
      await agentPage
        .getByRole("button", { name: "Move to In review" })
        .click();
      await expect(
        agentPage.getByText("changed the status from In progress to In review"),
      ).toBeVisible();
      await agentPage.getByRole("button", { name: "Move to Resolved" }).click();
      await expect(
        agentPage.getByText("changed the status from In review to Resolved"),
      ).toBeVisible();
    });

    await test.step("the reporter is not satisfied and reopens it", async () => {
      await reporterPage.reload();
      await reporterPage.getByRole("button", { name: "Reopen" }).click();
      await expect(
        reporterPage.getByText("changed the status from Resolved to Open"),
      ).toBeVisible();
      await expect(
        reporterPage.getByRole("button", { name: "Reopen" }),
      ).toHaveCount(0);
    });
  } finally {
    await reporterContext.close();
    await agentContext.close();
  }
});
