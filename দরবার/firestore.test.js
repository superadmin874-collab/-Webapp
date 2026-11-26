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
const SUPER_ADMIN_EMAIL = "realmepx36@gmail.com";
const ADMIN2_UID = "admin2_uid";
const ADMIN2_EMAIL = "admin2@dorbar.edu";
const STUDENT_ALICE_UID = "student_alice_uid";
const STUDENT_BOB_UID = "student_bob_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function superAdminDb() {
  return testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
}

function spoofedSuperAdminDb() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: SUPER_ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
}

function admin2Db() {
  return testEnv.authenticatedContext(ADMIN2_UID, {
    email: ADMIN2_EMAIL,
    email_verified: true,
  }).firestore();
}

function studentDb(uid) {
  return testEnv.authenticatedContext(uid, {
    email: `${uid}@student.local`,
    email_verified: true,
  }).firestore();
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("subjects").where("visible", "==", true).get());
  await assertFails(unauth.collection("documents").where("published", "==", true).get());
  await assertFails(unauth.collection("students").doc(STUDENT_ALICE_UID).get());
  await assertFails(unauth.collection("admins").doc(SUPER_ADMIN_UID).get());
  await assertFails(unauth.collection("app_settings").doc("global").get());
});

test("Email spoofing attack against Super Admin is rejected", async () => {
  const spoof = spoofedSuperAdminDb();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    spoof.collection("subjects").doc("bengali").set({
      nameEn: "Bengali",
      nameBn: "বাংলা",
      description: "Spoof attempt",
      iconKey: "book",
      accentHex: "#4F46E5",
      sortOrder: 1,
      visible: true,
      createdByUid: "spoof_uid",
      adminUids: ["spoof_uid"],
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Super Admin can create Admin 2, subjects, categories, documents, and protected settings", async () => {
  const db = superAdminDb();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    db.collection("admins").doc(SUPER_ADMIN_UID).set({
      uid: SUPER_ADMIN_UID,
      email: SUPER_ADMIN_EMAIL,
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      active: true,
      permissions: ["ALL"],
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID],
      lastLoginAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("admins").doc(ADMIN2_UID).set({
      uid: ADMIN2_UID,
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMINISTRATOR",
      active: true,
      permissions: ["MANAGE_DOCS", "MANAGE_QUIZ", "MANAGE_STUDENTS"],
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID, ADMIN2_UID],
      lastLoginAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("app_settings").doc("global").set({
      appName: "দরবার",
      tagline: "Your Digital Learning & Knowledge Hub",
      defaultThemeMode: "SYSTEM",
      maxUploadSizeMb: 5,
      allowStudentQuiz: true,
      allowStudentDownloads: true,
      dailyChallengeEnabled: true,
      notificationsEnabled: true,
      maintenanceMode: false,
      updatedByUid: SUPER_ADMIN_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("subjects").doc("physics").set({
      nameEn: "Physics",
      nameBn: "পদার্থবিদ্যা",
      description: "Mechanics, Optics, Electromagnetism",
      iconKey: "science",
      accentHex: "#6D28D9",
      sortOrder: 3,
      visible: true,
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID, ADMIN2_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin 2 can manage documents & quizzes, but cannot remove Super Admin or edit protected settings", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("admins").doc(SUPER_ADMIN_UID).set({
      uid: SUPER_ADMIN_UID,
      email: SUPER_ADMIN_EMAIL,
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      active: true,
      permissions: ["ALL"],
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID, ADMIN2_UID],
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("admins").doc(ADMIN2_UID).set({
      uid: ADMIN2_UID,
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMINISTRATOR",
      active: true,
      permissions: ["MANAGE_DOCS", "MANAGE_QUIZ"],
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID, ADMIN2_UID],
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("app_settings").doc("global").set({
      appName: "দরবার",
      tagline: "Your Digital Learning & Knowledge Hub",
      defaultThemeMode: "SYSTEM",
      maxUploadSizeMb: 5,
      allowStudentQuiz: true,
      allowStudentDownloads: true,
      dailyChallengeEnabled: true,
      notificationsEnabled: true,
      maintenanceMode: false,
      updatedByUid: SUPER_ADMIN_UID,
      updatedAt: now,
    });
  });

  const a2 = admin2Db();
  const now = new Date(Date.now() - 1000);

  // Admin 2 can upload a document
  await assertSucceeds(
    a2.collection("documents").doc("doc_1").set({
      title: "Optics Chapter 1 Notes",
      description: "Ray optics summary",
      subjectId: "physics",
      subjectName: "Physics",
      categoryId: "cat_notes",
      categoryName: "Chapter-wise Notes",
      chapter: "Chapter 1: Reflection",
      fileName: "optics.pdf",
      mimeType: "application/pdf",
      fileExtension: "pdf",
      fileSizeBytes: 1024,
      chunkCount: 1,
      tags: ["optics", "board"],
      featured: true,
      published: true,
      version: 1,
      downloadCount: 0,
      uploadedByUid: ADMIN2_UID,
      uploadedByName: "Admin Two",
      lastEditedByUid: ADMIN2_UID,
      lastEditedByName: "Admin Two",
      adminUids: [SUPER_ADMIN_UID, ADMIN2_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin 2 cannot delete Super Admin or change their own role
  await assertFails(a2.collection("admins").doc(SUPER_ADMIN_UID).delete());
  await assertFails(
    a2.collection("admins").doc(ADMIN2_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: now,
    })
  );

  // Admin 2 cannot modify protected security settings (maintenanceMode / maxUploadSizeMb)
  await assertFails(
    a2.collection("app_settings").doc("global").update({
      maintenanceMode: true,
      updatedByUid: ADMIN2_UID,
      updatedAt: now,
    })
  );

  // Admin 2 CAN update non-protected settings like tagline
  await assertSucceeds(
    a2.collection("app_settings").doc("global").update({
      tagline: "Digital Study Hub",
      updatedByUid: ADMIN2_UID,
      updatedAt: now,
    })
  );
});

test("Students can read only published documents, increment downloadCount by 1, and cannot spoof or cross-read", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("documents").doc("pub_doc").set({
      title: "Published PDF",
      description: "Visible to students",
      subjectId: "bengali",
      subjectName: "Bengali",
      categoryId: "cat_1",
      categoryName: "PDF Study Materials",
      chapter: "Chapter 1",
      fileName: "notes.pdf",
      mimeType: "application/pdf",
      fileExtension: "pdf",
      fileSizeBytes: 512,
      chunkCount: 1,
      tags: ["bengali"],
      featured: false,
      published: true,
      version: 1,
      downloadCount: 0,
      uploadedByUid: SUPER_ADMIN_UID,
      uploadedByName: "Super Admin",
      lastEditedByUid: SUPER_ADMIN_UID,
      lastEditedByName: "Super Admin",
      adminUids: [SUPER_ADMIN_UID],
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("documents").doc("draft_doc").set({
      title: "Draft PDF",
      description: "Hidden from students",
      subjectId: "bengali",
      subjectName: "Bengali",
      categoryId: "cat_1",
      categoryName: "PDF Study Materials",
      chapter: "Chapter 2",
      fileName: "draft.pdf",
      mimeType: "application/pdf",
      fileExtension: "pdf",
      fileSizeBytes: 512,
      chunkCount: 1,
      tags: [],
      featured: false,
      published: false,
      version: 1,
      downloadCount: 0,
      uploadedByUid: SUPER_ADMIN_UID,
      uploadedByName: "Super Admin",
      lastEditedByUid: SUPER_ADMIN_UID,
      lastEditedByName: "Super Admin",
      adminUids: [SUPER_ADMIN_UID],
      createdAt: now,
      updatedAt: now,
    });
  });

  const alice = studentDb(STUDENT_ALICE_UID);
  const bob = studentDb(STUDENT_BOB_UID);
  const now = new Date(Date.now() - 1000);

  // Student filtered query on published == true succeeds
  await assertSucceeds(alice.collection("documents").where("published", "==", true).get());
  // Unfiltered query by student fails
  await assertFails(alice.collection("documents").get());
  // Draft document get fails for student
  await assertFails(alice.collection("documents").doc("draft_doc").get());

  // Student can increment downloadCount by exactly +1 on published doc
  await assertSucceeds(
    alice.collection("documents").doc("pub_doc").update({
      downloadCount: 1,
    })
  );
  // Student cannot edit title or jump downloadCount
  await assertFails(
    alice.collection("documents").doc("pub_doc").update({
      title: "Hacked",
    })
  );
  await assertFails(
    alice.collection("documents").doc("pub_doc").update({
      downloadCount: 99,
    })
  );

  // Student creates own profile & subcollection records; Bob cannot read/write Alice's records
  await assertSucceeds(
    alice.collection("students").doc(STUDENT_ALICE_UID).set({
      uid: STUDENT_ALICE_UID,
      studentCode: "DRB-100201",
      fullName: "Ananya Sen",
      active: true,
      downloadCount: 0,
      quizAttemptsCount: 0,
      totalQuizScore: 0,
      totalQuizQuestions: 0,
      bestStreak: 0,
      achievements: [],
      lastActiveAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("students").doc(STUDENT_ALICE_UID).collection("favorites").doc("pub_doc").set({
      documentId: "pub_doc",
      title: "Published PDF",
      subjectId: "bengali",
      subjectName: "Bengali",
      fileExtension: "pdf",
      createdAt: now,
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    alice.collection("students").doc(STUDENT_ALICE_UID).set({
      uid: STUDENT_ALICE_UID,
      studentCode: "DRB-100201",
      fullName: "Ananya Sen",
      active: true,
      isAdmin: true,
      downloadCount: 0,
      quizAttemptsCount: 0,
      totalQuizScore: 0,
      totalQuizQuestions: 0,
      bestStreak: 0,
      achievements: [],
      lastActiveAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-student isolation
  await assertFails(bob.collection("students").doc(STUDENT_ALICE_UID).get());
  await assertFails(bob.collection("students").doc(STUDENT_ALICE_UID).collection("favorites").get());
  await assertFails(
    bob.collection("students").doc(STUDENT_ALICE_UID).update({
      fullName: "Hijacked",
      updatedAt: now,
    })
  );
});

test("Super Admin invite allows Admin 2 self-activation while blocking uninvited admin creation", async () => {
  const sa = superAdminDb();
  const a2 = admin2Db();
  const alice = studentDb(STUDENT_ALICE_UID);
  const now = new Date(Date.now() - 1000);
  const inviteId = "inv_admin2_dorbar_edu";

  await assertSucceeds(
    sa.collection("admin_invites").doc(inviteId).set({
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMINISTRATOR",
      permissions: ["MANAGE_DOCS", "MANAGE_QUIZ"],
      active: true,
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin 2 can claim the matching invite
  await assertSucceeds(
    a2.collection("admins").doc(ADMIN2_UID).set({
      uid: ADMIN2_UID,
      email: ADMIN2_EMAIL,
      displayName: "Admin Two",
      role: "ADMINISTRATOR",
      active: true,
      permissions: ["MANAGE_DOCS", "MANAGE_QUIZ"],
      createdByUid: SUPER_ADMIN_UID,
      adminUids: [SUPER_ADMIN_UID, ADMIN2_UID],
      lastLoginAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Uninvited student can check their own non-existent deterministic invite ID without PERMISSION_DENIED
  await assertSucceeds(
    alice.collection("admin_invites").doc("inv_student_alice_uid_student_local").get()
  );

  // Uninvited student cannot probe another user's invite ID
  await assertFails(
    alice.collection("admin_invites").doc(inviteId).get()
  );

  // Uninvited student cannot create their own admin record
  await assertFails(
    alice.collection("admins").doc(STUDENT_ALICE_UID).set({
      uid: STUDENT_ALICE_UID,
      email: `${STUDENT_ALICE_UID}@student.local`,
      displayName: "Rogue Admin",
      role: "ADMINISTRATOR",
      active: true,
      permissions: ["MANAGE_DOCS"],
      createdByUid: STUDENT_ALICE_UID,
      adminUids: [STUDENT_ALICE_UID],
      lastLoginAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );
});
