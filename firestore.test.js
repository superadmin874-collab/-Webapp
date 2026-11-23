const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_UID = "super_admin_uid";
const SUPER_ADMIN_EMAIL = "superadmin874@gmail.com";
const ADMIN2_UID = "admin2_uid";
const ADMIN2_EMAIL = "admin2@darbar.edu";
const STUDENT_ALICE_UID = "student_alice";
const STUDENT_BOB_UID = "student_bob";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function superAdminDb() {
  return testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function admin2Db() {
  return testEnv
    .authenticatedContext(ADMIN2_UID, {
      email: ADMIN2_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function aliceDb() {
  return testEnv
    .authenticatedContext(STUDENT_ALICE_UID, {
      email: "alice@student.edu",
      email_verified: true,
    })
    .firestore();
}

function bobDb() {
  return testEnv
    .authenticatedContext(STUDENT_BOB_UID, {
      email: "bob@student.edu",
      email_verified: true,
    })
    .firestore();
}

async function seedAdmin2() {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(SUPER_ADMIN_UID).set({
      uid: SUPER_ADMIN_UID,
      email: SUPER_ADMIN_EMAIL,
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      active: true,
      permissions: ["all"],
      createdBy: SUPER_ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("admins").doc(ADMIN2_UID).set({
      uid: ADMIN2_UID,
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMIN",
      active: true,
      permissions: [
        "manage_documents",
        "manage_subjects",
        "manage_categories",
        "manage_students",
        "manage_quizzes",
        "manage_announcements",
        "view_analytics",
        "manage_settings",
      ],
      createdBy: SUPER_ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated users are denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("documents").where("published", "==", true).get());
  await assertFails(unauthDb.collection("subjects").where("visible", "==", true).get());
  await assertFails(unauthDb.collection("students").doc(STUDENT_ALICE_UID).get());
});

test("Bootstrapped Super Admin can create own admin record, manage invites, and Admin 2 can claim invite", async () => {
  const now = new Date(Date.now() - 5000);
  const sa = superAdminDb();
  await assertSucceeds(
    sa.collection("admins").doc(SUPER_ADMIN_UID).set({
      uid: SUPER_ADMIN_UID,
      email: SUPER_ADMIN_EMAIL,
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      active: true,
      permissions: ["all"],
      createdBy: SUPER_ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Spoofed unverified email cannot pretend to be Super Admin
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: SUPER_ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  await assertFails(
    spoofDb.collection("admins").doc("spoof_uid").set({
      uid: "spoof_uid",
      email: SUPER_ADMIN_EMAIL,
      displayName: "Spoof",
      role: "SUPER_ADMIN",
      active: true,
      permissions: ["all"],
      createdBy: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Super Admin invites Admin 2
  await assertSucceeds(
    sa.collection("admin_invites").doc(ADMIN2_EMAIL).set({
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMIN",
      active: true,
      permissions: ["manage_documents", "manage_quizzes"],
      invitedByUid: SUPER_ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin 2 claims invite
  const a2 = admin2Db();
  await assertSucceeds(
    a2.collection("admins").doc(ADMIN2_UID).set({
      uid: ADMIN2_UID,
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMIN",
      active: true,
      permissions: ["manage_documents", "manage_quizzes"],
      createdBy: ADMIN2_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin 2 cannot delete Super Admin or modify protected security settings", async () => {
  await seedAdmin2();
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("app_settings").doc("global").set({
      appName: "দরবার",
      tagline: "Your Digital Learning & Knowledge Hub",
      defaultTheme: "SYSTEM",
      maxUploadMb: 5,
      allowStudentQuiz: true,
      allowStudentDownloads: true,
      notificationsEnabled: true,
      announcementsEnabled: true,
      maintenanceMode: false,
      requireStrictVerification: true,
      updatedBy: SUPER_ADMIN_UID,
      updatedAt: now,
    });
  });

  const a2 = admin2Db();
  // Admin 2 cannot delete or demote Super Admin
  await assertFails(a2.collection("admins").doc(SUPER_ADMIN_UID).delete());
  await assertFails(
    a2.collection("admins").doc(SUPER_ADMIN_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin 2 can update general settings (tagline), but NOT protected maintenanceMode
  await assertSucceeds(
    a2.collection("app_settings").doc("global").update({
      tagline: "Updated Learning Hub",
      updatedBy: ADMIN2_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    a2.collection("app_settings").doc("global").update({
      maintenanceMode: true,
      updatedBy: ADMIN2_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Super Admin CAN update maintenanceMode
  await assertSucceeds(
    superAdminDb().collection("app_settings").doc("global").update({
      maintenanceMode: true,
      updatedBy: SUPER_ADMIN_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Documents: published-only visibility for students, admin full control, safe download increment", async () => {
  await seedAdmin2();
  const now = new Date(Date.now() - 5000);
  const a2 = admin2Db();
  const alice = aliceDb();

  // Admin 2 uploads unpublished draft
  await assertSucceeds(
    a2.collection("documents").doc("doc_draft").set({
      title: "Draft Physics Notes",
      description: "Kinematics chapter 1",
      subjectId: "physics",
      subjectName: "Physics (পদার্থবিদ্যা)",
      categoryId: "chapter_notes",
      categoryName: "Chapter-wise Notes",
      chapter: "Chapter 1",
      tags: ["physics", "notes"],
      fileName: "kinematics.pdf",
      fileType: "PDF",
      mimeType: "application/pdf",
      fileSizeBytes: 12000,
      chunkCount: 1,
      published: false,
      featured: false,
      version: 1,
      downloadCount: 0,
      uploadedByUid: ADMIN2_UID,
      uploadedByName: "Admin Two",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    a2.collection("documents").doc("doc_draft").collection("chunks").doc("chunk_0").set({
      docId: "doc_draft",
      index: 0,
      data: "JVBERi0xLjQK",
    })
  );

  // Student cannot get unpublished doc or unfiltered list
  await assertFails(alice.collection("documents").doc("doc_draft").get());
  await assertFails(alice.collection("documents").get());
  await assertFails(
    alice.collection("documents").doc("doc_draft").collection("chunks").doc("chunk_0").get()
  );

  // Admin 1 (Super Admin) sees Admin 2's draft and publishes it (multi-admin sync)
  await assertSucceeds(
    superAdminDb().collection("documents").doc("doc_draft").update({
      published: true,
      version: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now student can read published doc via filtered query & read chunk_0
  await assertSucceeds(alice.collection("documents").where("published", "==", true).get());
  await assertSucceeds(
    alice.collection("documents").doc("doc_draft").collection("chunks").doc("chunk_0").get()
  );

  // Student can increment downloadCount by +1 only
  await assertSucceeds(
    alice.collection("documents").doc("doc_draft").update({
      downloadCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Student cannot tamper with title or jump downloadCount
  await assertFails(
    alice.collection("documents").doc("doc_draft").update({
      downloadCount: 999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    alice.collection("documents").doc("doc_draft").update({
      title: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Students: name-only profile & private subcollections isolated per user", async () => {
  await seedAdmin2();
  const now = new Date(Date.now() - 5000);
  const alice = aliceDb();
  const bob = bobDb();

  await assertSucceeds(
    alice.collection("students").doc(STUDENT_ALICE_UID).set({
      uid: STUDENT_ALICE_UID,
      studentCode: "DRB-102938",
      fullName: "Alice Rahman",
      disabled: false,
      downloadsCount: 0,
      quizzesTaken: 0,
      totalQuizScore: 0,
      totalQuestionsAnswered: 0,
      achievements: [],
      lastActiveAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can write her own favorite and quiz attempt
  await assertSucceeds(
    alice.collection("students").doc(STUDENT_ALICE_UID).collection("favorites").doc("doc_1").set({
      docId: "doc_1",
      title: "Bangla Grammar",
      subjectId: "bengali",
      subjectName: "Bengali (বাংলা)",
      fileType: "PDF",
      createdAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("students").doc(STUDENT_ALICE_UID).collection("quiz_attempts").doc("att_1").set({
      category: "Science and Technology",
      difficulty: "MEDIUM",
      mode: "Category",
      totalQuestions: 5,
      correctCount: 4,
      scorePercent: 80,
      durationSeconds: 45,
      createdAt: now,
    })
  );

  // Bob cannot read Alice's profile, favorites, or quiz attempts
  await assertFails(bob.collection("students").doc(STUDENT_ALICE_UID).get());
  await assertFails(bob.collection("students").get());
  await assertFails(
    bob.collection("students").doc(STUDENT_ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    bob.collection("students").doc(STUDENT_ALICE_UID).collection("quiz_attempts").get()
  );

  // Alice cannot self-disable/un-disable or change studentCode
  await assertFails(
    alice.collection("students").doc(STUDENT_ALICE_UID).update({
      studentCode: "DRB-000000",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin can list and disable student
  await assertSucceeds(admin2Db().collection("students").get());
  await assertSucceeds(
    admin2Db().collection("students").doc(STUDENT_ALICE_UID).update({
      disabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Quiz questions, announcements, and activity logs enforce role & approval gates", async () => {
  await seedAdmin2();
  const now = new Date(Date.now() - 5000);
  const a2 = admin2Db();
  const alice = aliceDb();

  // Unapproved AI question cannot be seen by students
  await assertSucceeds(
    a2.collection("quiz_questions").doc("q1").set({
      category: "West Bengal General Knowledge",
      difficulty: "MEDIUM",
      questionText: "Which river flows through Kolkata?",
      options: ["Hooghly", "Teesta", "Damodar", "Rupnarayan"],
      correctIndex: 0,
      explanation: "The Hooghly River flows alongside Kolkata.",
      source: "WB Textbook",
      factDate: "2026-11-23",
      aiGenerated: true,
      verifiedByAdmin: false,
      approved: false,
      createdByUid: ADMIN2_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(alice.collection("quiz_questions").doc("q1").get());

  // Approve question
  await assertSucceeds(
    a2.collection("quiz_questions").doc("q1").update({
      verifiedByAdmin: true,
      approved: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(alice.collection("quiz_questions").where("approved", "==", true).get());

  // Activity log: Admin can create & read, only Super Admin can delete
  await assertSucceeds(
    a2.collection("activity_logs").doc("log_1").set({
      adminUid: ADMIN2_UID,
      adminName: "Admin Two",
      adminRole: "ADMIN",
      action: "APPROVE_QUIZ",
      targetType: "quiz_question",
      targetId: "q1",
      details: "Approved AI-generated GK question",
      createdAt: now,
    })
  );
  await assertFails(alice.collection("activity_logs").get());
  await assertFails(a2.collection("activity_logs").doc("log_1").delete());
  await assertSucceeds(superAdminDb().collection("activity_logs").doc("log_1").delete());
});
