import { test, before, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const PROJECT_ID = 'demo-test';
const DATABASE_ID = 'ai-studio-android-smartfin-8bea01c1-7401-4c50-8ad3-14afa960619b';
const HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085';
const FIRESTORE_BASE = `http://${HOST}/v1/projects/${PROJECT_ID}/databases/${DATABASE_ID}/documents`;

function base64Url(obj) {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function makeFakeJwt(uid, email, emailVerified = true) {
  const header = base64Url({ alg: 'none', typ: 'JWT' });
  const nowSec = Math.floor(Date.now() / 1000);
  const payload = base64Url({
    iss: `https://securetoken.google.com/${PROJECT_ID}`,
    aud: PROJECT_ID,
    auth_time: nowSec - 60,
    user_id: uid,
    sub: uid,
    iat: nowSec - 60,
    exp: nowSec + 3600,
    email,
    email_verified: emailVerified,
    firebase: { identities: { email: [email] }, sign_in_provider: 'google.com' },
  });
  return `${header}.${payload}.`;
}

async function pushRules() {
  const rulesContent = readFileSync('firestore.rules', 'utf8');
  const res = await fetch(
    `http://${HOST}/emulator/v1/projects/${PROJECT_ID}:securityRules?database=projects/${PROJECT_ID}/databases/${DATABASE_ID}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ignore_errors: false,
        rules: {
          files: [{ name: 'firestore.rules', content: rulesContent }],
        },
      }),
    }
  );
  const body = await res.text();
  assert.equal(res.status, 200, `Failed to push rules: ${body}`);
}

async function clearFirestore() {
  const res = await fetch(
    `http://${HOST}/emulator/v1/projects/${PROJECT_ID}/databases/${DATABASE_ID}/documents`,
    { method: 'DELETE' }
  );
  assert.equal(res.status, 200);
}

function isoPast(msAgo = 2000) {
  return new Date(Date.now() - msAgo).toISOString();
}

function isoFuture(msAhead = 120000) {
  return new Date(Date.now() + msAhead).toISOString();
}

function buildBackupDoc(uid, email, overrides = {}) {
  const ts = isoPast();
  return {
    fields: {
      userId: { stringValue: uid },
      email: { stringValue: email },
      payloadJson: { stringValue: '{"version":2,"scope":"FULL_DATABASE"}' },
      exportedAt: { integerValue: '1731580000000' },
      accountCount: { integerValue: '6' },
      transactionCount: { integerValue: '15' },
      schemaVersion: { integerValue: '2' },
      createdAt: { timestampValue: ts },
      updatedAt: { timestampValue: ts },
      ...overrides,
    },
  };
}

function buildWalletDoc(uid, overrides = {}) {
  const ts = isoPast();
  return {
    fields: {
      userId: { stringValue: uid },
      tournamentBalance: { doubleValue: 250.0 },
      gameBalance: { doubleValue: 100.0 },
      equippedBoardSkinId: { stringValue: 'skin_royal_emerald' },
      equippedAvatarId: { stringValue: 'avatar_monarch' },
      ownedItemsCsv: { stringValue: 'skin_royal_emerald,avatar_monarch' },
      createdAt: { timestampValue: ts },
      updatedAt: { timestampValue: ts },
      ...overrides,
    },
  };
}

function buildWalletRequestDoc(uid, overrides = {}) {
  const ts = isoPast();
  return {
    fields: {
      userId: { stringValue: uid },
      requestType: { stringValue: 'DEPOSIT' },
      channel: { stringValue: 'BKASH' },
      amount: { doubleValue: 500.0 },
      accountNumber: { stringValue: '01711223344' },
      transactionId: { stringValue: 'TRX998877' },
      status: { stringValue: 'PENDING' },
      adminNote: { stringValue: '' },
      createdAt: { timestampValue: ts },
      updatedAt: { timestampValue: ts },
      ...overrides,
    },
  };
}

function buildRoomDoc(roomCode, hostUid, overrides = {}) {
  const ts = isoPast();
  return {
    fields: {
      roomCode: { stringValue: roomCode },
      gameType: { stringValue: 'CHESS' },
      hostUid: { stringValue: hostUid },
      hostName: { stringValue: 'Alice' },
      guestUid: { stringValue: '' },
      guestName: { stringValue: '' },
      memberUids: { arrayValue: { values: [{ stringValue: hostUid }] } },
      publicRoom: { booleanValue: true },
      entryFee: { doubleValue: 50.0 },
      status: { stringValue: 'WAITING' },
      turnUid: { stringValue: hostUid },
      winnerUid: { stringValue: '' },
      statePayload: { stringValue: '{"turn":"WHITE"}' },
      moveCount: { integerValue: '0' },
      lastMoveSummary: { stringValue: 'Room created' },
      createdAt: { timestampValue: ts },
      updatedAt: { timestampValue: ts },
      ...overrides,
    },
  };
}

const aliceToken = makeFakeJwt('alice_uid', 'alice@gmail.com', true);
const bobToken = makeFakeJwt('bob_uid', 'bob@gmail.com', true);
const charlieToken = makeFakeJwt('charlie_uid', 'charlie@gmail.com', true);
const adminToken = makeFakeJwt('admin_uid', 'abulha1975shem1975@gmail.com', true);
const unverifiedAdminToken = makeFakeJwt('fake_admin', 'abulha1975shem1975@gmail.com', false);

before(async () => {
  await pushRules();
});

beforeEach(async () => {
  await clearFirestore();
});

test('default-deny blocks unknown collections and unauthenticated traffic', async () => {
  const unknownRes = await fetch(`${FIRESTORE_BASE}/secrets/doc1`, {
    headers: { Authorization: `Bearer ${aliceToken}` },
  });
  assert.equal(unknownRes.status, 403);

  const unauthBackup = await fetch(`${FIRESTORE_BASE}/users/alice_uid/backups/latest`);
  assert.equal(unauthBackup.status, 403);

  const unauthRoom = await fetch(`${FIRESTORE_BASE}/multiplayer_rooms/ABC123`);
  assert.equal(unauthRoom.status, 403);
});

test('CloudBackupVault owner CRUD works and blocks cross-user + shadow fields', async () => {
  const url = `${FIRESTORE_BASE}/users/alice_uid/backups/latest`;

  const createRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildBackupDoc('alice_uid', 'alice@gmail.com')),
  });
  assert.equal(createRes.status, 200, await createRes.text());

  const bobRead = await fetch(url, { headers: { Authorization: `Bearer ${bobToken}` } });
  assert.equal(bobRead.status, 403);

  const shadowRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildBackupDoc('alice_uid', 'alice@gmail.com', { hack: { booleanValue: true } })),
  });
  assert.equal(shadowRes.status, 403);
});

test('DualWalletSummary allows owner write, blocks negative balances, future timestamps, and other users', async () => {
  const url = `${FIRESTORE_BASE}/users/alice_uid/wallet/primary`;

  const createRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildWalletDoc('alice_uid')),
  });
  assert.equal(createRes.status, 200, await createRes.text());

  const bobRead = await fetch(url, { headers: { Authorization: `Bearer ${bobToken}` } });
  assert.equal(bobRead.status, 403);

  const negRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildWalletDoc('alice_uid', { tournamentBalance: { doubleValue: -10.0 } })),
  });
  assert.equal(negRes.status, 403);

  const futureRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildWalletDoc('alice_uid', { updatedAt: { timestampValue: isoFuture() } })),
  });
  assert.equal(futureRes.status, 403);
});

test('WalletRequestDoc allows user PENDING create, blocks self-approval, and allows verified Admin approval only', async () => {
  const url = `${FIRESTORE_BASE}/wallet_requests/req_001`;

  // User cannot create pre-approved request
  const cheatCreate = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildWalletRequestDoc('alice_uid', { status: { stringValue: 'APPROVED' } })),
  });
  assert.equal(cheatCreate.status, 403);

  // User creates PENDING request
  const createRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildWalletRequestDoc('alice_uid')),
  });
  assert.equal(createRes.status, 200, await createRes.text());

  // User cannot self-approve
  const selfApprove = await fetch(`${url}?updateMask.fieldPaths=status&updateMask.fieldPaths=adminNote&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        status: { stringValue: 'APPROVED' },
        adminNote: { stringValue: 'self' },
        updatedAt: { timestampValue: isoPast(500) },
      },
    }),
  });
  assert.equal(selfApprove.status, 403);

  // Unverified admin email is rejected
  const fakeAdminApprove = await fetch(`${url}?updateMask.fieldPaths=status&updateMask.fieldPaths=adminNote&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${unverifiedAdminToken}` },
    body: JSON.stringify({
      fields: {
        status: { stringValue: 'APPROVED' },
        adminNote: { stringValue: 'fake' },
        updatedAt: { timestampValue: isoPast(500) },
      },
    }),
  });
  assert.equal(fakeAdminApprove.status, 403);

  // Verified admin approves
  const realAdminApprove = await fetch(`${url}?updateMask.fieldPaths=status&updateMask.fieldPaths=adminNote&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` },
    body: JSON.stringify({
      fields: {
        status: { stringValue: 'APPROVED' },
        adminNote: { stringValue: 'Trx verified' },
        updatedAt: { timestampValue: isoPast(500) },
      },
    }),
  });
  assert.equal(realAdminApprove.status, 200, await realAdminApprove.text());
});

test('MultiplayerRoomDoc lifecycle: create -> guest join -> turn-enforced move -> terminal state lock -> outsider blocked', async () => {
  const url = `${FIRESTORE_BASE}/multiplayer_rooms/K7M9P2`;

  // 1. Host creates WAITING room
  const createRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildRoomDoc('K7M9P2', 'alice_uid')),
  });
  assert.equal(createRes.status, 200, await createRes.text());

  // 2. Public Quick Match query succeeds for Bob
  const queryRes = await fetch(`${FIRESTORE_BASE}:runQuery`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: 'multiplayer_rooms' }],
        where: {
          compositeFilter: {
            op: 'AND',
            filters: [
              { fieldFilter: { field: { fieldPath: 'status' }, op: 'EQUAL', value: { stringValue: 'WAITING' } } },
              { fieldFilter: { field: { fieldPath: 'publicRoom' }, op: 'EQUAL', value: { booleanValue: true } } },
              { fieldFilter: { field: { fieldPath: 'gameType' }, op: 'EQUAL', value: { stringValue: 'CHESS' } } },
            ],
          },
        },
      },
    }),
  });
  assert.equal(queryRes.status, 200, await queryRes.text());

  // 3. Bob joins the WAITING room
  const joinRes = await fetch(
    `${url}?updateMask.fieldPaths=guestUid&updateMask.fieldPaths=guestName&updateMask.fieldPaths=memberUids&updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
      body: JSON.stringify({
        fields: {
          guestUid: { stringValue: 'bob_uid' },
          guestName: { stringValue: 'Bob' },
          memberUids: { arrayValue: { values: [{ stringValue: 'alice_uid' }, { stringValue: 'bob_uid' }] } },
          status: { stringValue: 'PLAYING' },
          updatedAt: { timestampValue: isoPast(1000) },
        },
      }),
    }
  );
  assert.equal(joinRes.status, 200, await joinRes.text());

  // 4. Outsider Charlie cannot read active PLAYING room
  const charlieRead = await fetch(url, { headers: { Authorization: `Bearer ${charlieToken}` } });
  assert.equal(charlieRead.status, 403);

  // 5. Bob cannot move out-of-turn (it is Alice's turn)
  const bobOutOfTurn = await fetch(
    `${url}?updateMask.fieldPaths=statePayload&updateMask.fieldPaths=turnUid&updateMask.fieldPaths=moveCount&updateMask.fieldPaths=lastMoveSummary&updateMask.fieldPaths=status&updateMask.fieldPaths=winnerUid&updateMask.fieldPaths=updatedAt`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
      body: JSON.stringify({
        fields: {
          statePayload: { stringValue: '{"turn":"WHITE","hack":1}' },
          turnUid: { stringValue: 'alice_uid' },
          moveCount: { integerValue: '1' },
          lastMoveSummary: { stringValue: 'illegal' },
          status: { stringValue: 'PLAYING' },
          winnerUid: { stringValue: '' },
          updatedAt: { timestampValue: isoPast(800) },
        },
      }),
    }
  );
  assert.equal(bobOutOfTurn.status, 403);

  // 6. Alice submits legal winning move -> FINISHED
  const aliceMove = await fetch(
    `${url}?updateMask.fieldPaths=statePayload&updateMask.fieldPaths=turnUid&updateMask.fieldPaths=moveCount&updateMask.fieldPaths=lastMoveSummary&updateMask.fieldPaths=status&updateMask.fieldPaths=winnerUid&updateMask.fieldPaths=updatedAt`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
      body: JSON.stringify({
        fields: {
          statePayload: { stringValue: '{"turn":"BLACK","outcome":"WHITE_WINS"}' },
          turnUid: { stringValue: 'bob_uid' },
          moveCount: { integerValue: '1' },
          lastMoveSummary: { stringValue: 'Checkmate!' },
          status: { stringValue: 'FINISHED' },
          winnerUid: { stringValue: 'alice_uid' },
          updatedAt: { timestampValue: isoPast(500) },
        },
      }),
    }
  );
  assert.equal(aliceMove.status, 200, await aliceMove.text());

  // 7. Terminal state lock: Bob cannot mutate a FINISHED match
  const mutateFinished = await fetch(
    `${url}?updateMask.fieldPaths=statePayload&updateMask.fieldPaths=turnUid&updateMask.fieldPaths=moveCount&updateMask.fieldPaths=lastMoveSummary&updateMask.fieldPaths=status&updateMask.fieldPaths=winnerUid&updateMask.fieldPaths=updatedAt`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
      body: JSON.stringify({
        fields: {
          statePayload: { stringValue: '{"turn":"WHITE"}' },
          turnUid: { stringValue: 'alice_uid' },
          moveCount: { integerValue: '2' },
          lastMoveSummary: { stringValue: 'reopen' },
          status: { stringValue: 'PLAYING' },
          winnerUid: { stringValue: '' },
          updatedAt: { timestampValue: isoPast(200) },
        },
      }),
    }
  );
  assert.equal(mutateFinished.status, 403);
});

test('WalletLedgerDoc owner read/create works, blocks cross-user reads, updates, and invalid buckets', async () => {
  const url = `${FIRESTORE_BASE}/users/alice_uid/wallet_ledger/led_001`;
  const ts = isoPast();
  const validLedger = {
    fields: {
      userId: { stringValue: 'alice_uid' },
      walletBucket: { stringValue: 'TOURNAMENT' },
      kind: { stringValue: 'DEPOSIT_APPROVED' },
      amountDelta: { doubleValue: 500.0 },
      note: { stringValue: 'Binance USDT deposit approved' },
      createdAt: { timestampValue: ts },
    },
  };

  const createRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(validLedger),
  });
  assert.equal(createRes.status, 200, await createRes.text());

  // Owner can read
  const ownerRead = await fetch(url, { headers: { Authorization: `Bearer ${aliceToken}` } });
  assert.equal(ownerRead.status, 200);

  // Other user blocked
  const bobRead = await fetch(url, { headers: { Authorization: `Bearer ${bobToken}` } });
  assert.equal(bobRead.status, 403);

  // Immutable: updates blocked even by owner
  const updateBlocked = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(validLedger),
  });
  assert.equal(updateBlocked.status, 403);

  // Invalid bucket rejected
  const badBucket = await fetch(`${FIRESTORE_BASE}/users/alice_uid/wallet_ledger/led_002`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        ...validLedger.fields,
        walletBucket: { stringValue: 'HACKED' },
      },
    }),
  });
  assert.equal(badBucket.status, 403);
});

test('BINANCE WalletRequestDoc with TRC20 address and DualWalletSummary winningBalance succeed', async () => {
  const reqUrl = `${FIRESTORE_BASE}/wallet_requests/req_binance_01`;
  const reqRes = await fetch(reqUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(
      buildWalletRequestDoc('alice_uid', {
        channel: { stringValue: 'BINANCE' },
        accountNumber: { stringValue: 'TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE' },
        transactionId: { stringValue: 'BINPAY-884422' },
      })
    ),
  });
  assert.equal(reqRes.status, 200, await reqRes.text());

  const walletUrl = `${FIRESTORE_BASE}/users/alice_uid/wallet/primary`;
  const walletRes = await fetch(walletUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(
      buildWalletDoc('alice_uid', {
        winningBalance: { doubleValue: 320.0 },
      })
    ),
  });
  assert.equal(walletRes.status, 200, await walletRes.text());
});

test('BroadcastDoc allows verified Admin create and signed-in read, blocks non-admin create and unauthenticated read', async () => {
  const url = `${FIRESTORE_BASE}/broadcasts/bc_001`;
  const body = JSON.stringify({
    fields: {
      title: { stringValue: 'Free Fire Bermuda Cup Room Open!' },
      message: { stringValue: 'Room ID: 882194 | Pass: 7744 — Join within 10 minutes!' },
      priority: { stringValue: 'URGENT' },
      senderEmail: { stringValue: 'abulha1975shem1975@gmail.com' },
      createdAt: { timestampValue: isoPast() },
    },
  });

  // Regular user cannot publish broadcasts
  const userCreate = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body,
  });
  assert.equal(userCreate.status, 403);

  // Verified admin can publish broadcast
  const adminCreate = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` },
    body,
  });
  assert.equal(adminCreate.status, 200, await adminCreate.text());

  // Any signed-in user can read broadcast
  const aliceRead = await fetch(url, { headers: { Authorization: `Bearer ${aliceToken}` } });
  assert.equal(aliceRead.status, 200);

  // Unauthenticated client cannot read broadcast
  const unauthRead = await fetch(url);
  assert.equal(unauthRead.status, 403);
});

function buildTournamentDoc(overrides = {}) {
  const ts = isoPast();
  const startMs = Date.now() + 3600000;
  return {
    fields: {
      title: { stringValue: 'Free Fire Bermuda Pro Cup' },
      gameType: { stringValue: 'FREE_FIRE' },
      gameCategory: { stringValue: 'FREE_FIRE' },
      modeLabel: { stringValue: 'SQUAD' },
      mapName: { stringValue: 'Bermuda' },
      bannerKey: { stringValue: 'BERMUDA' },
      entryFee: { doubleValue: 30.0 },
      prizePool: { doubleValue: 1000.0 },
      playerUids: { arrayValue: {} },
      startTime: { timestampValue: new Date(startMs).toISOString() },
      perKillReward: { doubleValue: 15.0 },
      prizeRanksJson: { stringValue: '[{"rankLabel":"1st Prize","amount":500}]' },
      maxSlots: { integerValue: '48' },
      joinedCount: { integerValue: '0' },
      startTimeLabel: { stringValue: 'Tonight 9:00 PM' },
      matchStartTimeMillis: { integerValue: String(startMs) },
      preMatchAlertMinutes: { integerValue: '10' },
      roomAlertSent: { booleanValue: false },
      versionLabel: { stringValue: 'Mobile Only' },
      rulesNotice: { stringValue: 'No hacks or teaming.' },
      adminCommissionPercent: { doubleValue: 20.0 },
      roomIdCode: { stringValue: '' },
      roomPassword: { stringValue: '' },
      status: { stringValue: 'UPCOMING' },
      winnerName: { stringValue: '' },
      localRoomId: { integerValue: '1' },
      createdAt: { timestampValue: ts },
      updatedAt: { timestampValue: ts },
      ...overrides,
    },
  };
}

test('TournamentDoc & Entries: Admin manages lifecycle, player atomic playerUids join succeeds, tampering & unauth blocked', async () => {
  const docId = 'tour_ff_001';
  const docPath = `projects/${PROJECT_ID}/databases/${DATABASE_ID}/documents/tournaments/${docId}`;
  const entryPath = `${docPath}/entries/alice_uid`;
  const url = `${FIRESTORE_BASE}/tournaments/${docId}`;

  // 1. Regular user cannot create a tournament
  const userCreate = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify(buildTournamentDoc()),
  });
  assert.equal(userCreate.status, 403);

  // 2. Verified Admin creates tournament with canonical gameType, playerUids, startTime timestamp
  const adminCreate = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` },
    body: JSON.stringify(buildTournamentDoc()),
  });
  assert.equal(adminCreate.status, 200, await adminCreate.text());

  // 2b. Invalid gameType rejected even for Admin
  const badGameType = await fetch(`${FIRESTORE_BASE}/tournaments/tour_bad_game`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` },
    body: JSON.stringify(buildTournamentDoc({ gameType: { stringValue: 'UNKNOWN_GAME' } })),
  });
  assert.equal(badGameType.status, 403);

  // 3. Unauthenticated cannot list or read tournaments
  const unauthRead = await fetch(url);
  assert.equal(unauthRead.status, 403);

  // 4. Signed-in player can read tournament
  const aliceRead = await fetch(url, { headers: { Authorization: `Bearer ${aliceToken}` } });
  assert.equal(aliceRead.status, 200);

  // 5. Player cannot tamper with prizePool or status
  const tamperPrize = await fetch(`${url}?updateMask.fieldPaths=prizePool&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        prizePool: { doubleValue: 999999.0 },
        updatedAt: { timestampValue: isoPast(500) },
      },
    }),
  });
  assert.equal(tamperPrize.status, 403);

  // 6. Player cannot increment joinedCount without atomically appending own UID + creating entry doc
  const fakeInc = await fetch(`${url}?updateMask.fieldPaths=joinedCount&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        joinedCount: { integerValue: '1' },
        updatedAt: { timestampValue: isoPast(500) },
      },
    }),
  });
  assert.equal(fakeInc.status, 403);

  // 7. Atomic batch commit: Alice creates /entries/alice_uid AND appends alice_uid to playerUids + joinedCount +1
  const ts = isoPast(400);
  const commitRes = await fetch(`${FIRESTORE_BASE}:commit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      writes: [
        {
          update: {
            name: entryPath,
            fields: {
              tournamentId: { stringValue: docId },
              userId: { stringValue: 'alice_uid' },
              usernameSnapshot: { stringValue: 'alice' },
              inGameName: { stringValue: 'FF_AlicePro' },
              inGameUid: { stringValue: '99887766' },
              entryFeePaid: { doubleValue: 30.0 },
              joinedAt: { timestampValue: ts },
            },
          },
          currentDocument: { exists: false },
        },
        {
          update: {
            name: docPath,
            fields: {
              playerUids: { arrayValue: { values: [{ stringValue: 'alice_uid' }] } },
              joinedCount: { integerValue: '1' },
              updatedAt: { timestampValue: ts },
            },
          },
          updateMask: { fieldPaths: ['playerUids', 'joinedCount', 'updatedAt'] },
        },
      ],
    }),
  });
  assert.equal(commitRes.status, 200, await commitRes.text());

  // 7b. Bob cannot wipe Alice out of playerUids while joining
  const bobWipeAlice = await fetch(`${FIRESTORE_BASE}:commit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
    body: JSON.stringify({
      writes: [
        {
          update: {
            name: `${docPath}/entries/bob_uid`,
            fields: {
              tournamentId: { stringValue: docId },
              userId: { stringValue: 'bob_uid' },
              usernameSnapshot: { stringValue: 'bob' },
              inGameName: { stringValue: 'BobHacker' },
              inGameUid: { stringValue: '55443322' },
              entryFeePaid: { doubleValue: 30.0 },
              joinedAt: { timestampValue: isoPast(350) },
            },
          },
          currentDocument: { exists: false },
        },
        {
          update: {
            name: docPath,
            fields: {
              playerUids: { arrayValue: { values: [{ stringValue: 'bob_uid' }] } },
              joinedCount: { integerValue: '2' },
              updatedAt: { timestampValue: isoPast(350) },
            },
          },
          updateMask: { fieldPaths: ['playerUids', 'joinedCount', 'updatedAt'] },
        },
      ],
    }),
  });
  assert.equal(bobWipeAlice.status, 403);

  // 8. Signed-in Bob can view live joined players roster
  const bobReadEntry = await fetch(`${url}/entries/alice_uid`, {
    headers: { Authorization: `Bearer ${bobToken}` },
  });
  assert.equal(bobReadEntry.status, 200);

  // 9. Bob cannot spoof Alice's entry ID
  const bobSpoof = await fetch(`${url}/entries/alice_uid_spoof`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
    body: JSON.stringify({
      fields: {
        tournamentId: { stringValue: docId },
        userId: { stringValue: 'bob_uid' },
        usernameSnapshot: { stringValue: 'bob' },
        inGameName: { stringValue: 'Spoof' },
        inGameUid: { stringValue: '11223344' },
        entryFeePaid: { doubleValue: 30.0 },
        joinedAt: { timestampValue: isoPast(300) },
      },
    }),
  });
  assert.equal(bobSpoof.status, 403);

  // 10. Verified Admin updates room credentials & marks COMPLETED
  const adminComplete = await fetch(
    `${url}?updateMask.fieldPaths=roomIdCode&updateMask.fieldPaths=roomPassword&updateMask.fieldPaths=roomAlertSent&updateMask.fieldPaths=status&updateMask.fieldPaths=winnerName&updateMask.fieldPaths=updatedAt`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` },
      body: JSON.stringify({
        fields: {
          roomIdCode: { stringValue: '884422' },
          roomPassword: { stringValue: '7777' },
          roomAlertSent: { booleanValue: true },
          status: { stringValue: 'COMPLETED' },
          winnerName: { stringValue: 'FF_AlicePro' },
          updatedAt: { timestampValue: isoPast(200) },
        },
      }),
    }
  );
  assert.equal(adminComplete.status, 200, await adminComplete.text());

  // 11. Terminal lock: Bob cannot join a COMPLETED tournament
  const bobJoinCompleted = await fetch(`${FIRESTORE_BASE}:commit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
    body: JSON.stringify({
      writes: [
        {
          update: {
            name: `${docPath}/entries/bob_uid`,
            fields: {
              tournamentId: { stringValue: docId },
              userId: { stringValue: 'bob_uid' },
              usernameSnapshot: { stringValue: 'bob' },
              inGameName: { stringValue: 'LateBob' },
              inGameUid: { stringValue: '44556677' },
              entryFeePaid: { doubleValue: 30.0 },
              joinedAt: { timestampValue: isoPast(100) },
            },
          },
          currentDocument: { exists: false },
        },
        {
          update: {
            name: docPath,
            fields: {
              playerUids: { arrayValue: { values: [{ stringValue: 'alice_uid' }, { stringValue: 'bob_uid' }] } },
              joinedCount: { integerValue: '2' },
              updatedAt: { timestampValue: isoPast(100) },
            },
          },
          updateMask: { fieldPaths: ['playerUids', 'joinedCount', 'updatedAt'] },
        },
      ],
    }),
  });
  assert.equal(bobJoinCompleted.status, 403);
});

test('Minimal canonical Tournament model (title, entryFee, prizePool, playerUids, startTime, gameType=LUDO) succeeds and supports self-only join', async () => {
  const docId = 'tour_ludo_5p_001';
  const url = `${FIRESTORE_BASE}/tournaments/${docId}`;
  const ts = isoPast(1000);
  const startIso = isoFuture(7200000);

  // Admin creates minimal canonical Tournament doc for 5-Player Ludo
  const createRes = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${adminToken}` },
    body: JSON.stringify({
      fields: {
        title: { stringValue: '5-Player Star Ludo Grand Cup' },
        entryFee: { doubleValue: 25.0 },
        prizePool: { doubleValue: 110.0 },
        playerUids: { arrayValue: {} },
        startTime: { timestampValue: startIso },
        gameType: { stringValue: 'LUDO' },
        maxSlots: { integerValue: '5' },
        status: { stringValue: 'UPCOMING' },
        createdAt: { timestampValue: ts },
        updatedAt: { timestampValue: ts },
      },
    }),
  });
  assert.equal(createRes.status, 200, await createRes.text());

  // Alice joins by appending only her own UID to playerUids
  const aliceJoin = await fetch(`${url}?updateMask.fieldPaths=playerUids&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        playerUids: { arrayValue: { values: [{ stringValue: 'alice_uid' }] } },
        updatedAt: { timestampValue: isoPast(500) },
      },
    }),
  });
  assert.equal(aliceJoin.status, 200, await aliceJoin.text());

  // Alice cannot double-join
  const aliceDouble = await fetch(`${url}?updateMask.fieldPaths=playerUids&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        playerUids: { arrayValue: { values: [{ stringValue: 'alice_uid' }, { stringValue: 'alice_uid' }] } },
        updatedAt: { timestampValue: isoPast(300) },
      },
    }),
  });
  assert.equal(aliceDouble.status, 403);

  // Bob cannot add Charlie instead of himself
  const bobAddCharlie = await fetch(`${url}?updateMask.fieldPaths=playerUids&updateMask.fieldPaths=updatedAt`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
    body: JSON.stringify({
      fields: {
        playerUids: { arrayValue: { values: [{ stringValue: 'alice_uid' }, { stringValue: 'charlie_uid' }] } },
        updatedAt: { timestampValue: isoPast(200) },
      },
    }),
  });
  assert.equal(bobAddCharlie.status, 403);
});

test('Tournament Leaderboard (/tournament_leaderboard/{userId}): signed-in Top 10 ranking query, owner bounded updates, anti-cheat & admin controls', async () => {
  const aliceUrl = `${FIRESTORE_BASE}/tournament_leaderboard/alice_uid`;
  const bobUrl = `${FIRESTORE_BASE}/tournament_leaderboard/bob_uid`;
  const ts = isoPast(1000);

  // 1. Unauthenticated read must fail
  const unauthRead = await fetch(`${FIRESTORE_BASE}/tournament_leaderboard`);
  assert.equal(unauthRead.status, 403);

  // 2. Owner (Alice) can create her own leaderboard profile with bounded initial matchPoints
  const aliceCreate = await fetch(aliceUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        userId: { stringValue: 'alice_uid' },
        displayName: { stringValue: 'Alice Pro' },
        avatarId: { stringValue: 'avatar_monarch' },
        matchPoints: { integerValue: '650' },
        matchesPlayed: { integerValue: '1' },
        wins: { integerValue: '1' },
        totalCoinsEarned: { doubleValue: 120.0 },
        favoriteGame: { stringValue: 'WAR_LAND' },
        createdAt: { timestampValue: ts },
        updatedAt: { timestampValue: ts },
      },
    }),
  });
  assert.equal(aliceCreate.status, 200, await aliceCreate.text());

  // 3. Owner (Bob) creates a higher-scoring leaderboard entry
  const bobCreate = await fetch(bobUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${bobToken}` },
    body: JSON.stringify({
      fields: {
        userId: { stringValue: 'bob_uid' },
        displayName: { stringValue: 'Bob Champion' },
        avatarId: { stringValue: 'avatar_cyber' },
        matchPoints: { integerValue: '1450' },
        matchesPlayed: { integerValue: '2' },
        wins: { integerValue: '2' },
        totalCoinsEarned: { doubleValue: 350.0 },
        favoriteGame: { stringValue: 'LUDO' },
        createdAt: { timestampValue: ts },
        updatedAt: { timestampValue: ts },
      },
    }),
  });
  assert.equal(bobCreate.status, 200, await bobCreate.text());

  // 4. Signed-in user can run real-time Top 10 query ordered by matchPoints DESC
  const top10Res = await fetch(`${FIRESTORE_BASE}:runQuery`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: 'tournament_leaderboard' }],
        orderBy: [{ field: { fieldPath: 'matchPoints' }, direction: 'DESCENDING' }],
        limit: 10,
      },
    }),
  });
  const top10Body = await top10Res.text();
  assert.equal(top10Res.status, 200, top10Body);
  const top10Rows = JSON.parse(top10Body);
  const rankedNames = top10Rows
    .filter((r) => r.document)
    .map((r) => r.document.fields.displayName.stringValue);
  assert.deepEqual(rankedNames, ['Bob Champion', 'Alice Pro']);

  // 5. Alice can monotonically increment her own matchPoints within anti-cheat bounds
  const aliceUpdate = await fetch(aliceUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        userId: { stringValue: 'alice_uid' },
        displayName: { stringValue: 'Alice Pro' },
        avatarId: { stringValue: 'avatar_monarch' },
        matchPoints: { integerValue: '1200' },
        matchesPlayed: { integerValue: '2' },
        wins: { integerValue: '2' },
        totalCoinsEarned: { doubleValue: 260.0 },
        favoriteGame: { stringValue: 'FREE_FIRE' },
        createdAt: { timestampValue: ts },
        updatedAt: { timestampValue: isoPast(400) },
      },
    }),
  });
  assert.equal(aliceUpdate.status, 200, await aliceUpdate.text());

  // 6. Anti-cheat: Alice cannot jump +99999 matchPoints in a single update
  const aliceCheat = await fetch(aliceUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        userId: { stringValue: 'alice_uid' },
        displayName: { stringValue: 'Alice Pro' },
        avatarId: { stringValue: 'avatar_monarch' },
        matchPoints: { integerValue: '999999' },
        matchesPlayed: { integerValue: '3' },
        wins: { integerValue: '3' },
        totalCoinsEarned: { doubleValue: 300.0 },
        favoriteGame: { stringValue: 'FREE_FIRE' },
        createdAt: { timestampValue: ts },
        updatedAt: { timestampValue: isoPast(200) },
      },
    }),
  });
  assert.equal(aliceCheat.status, 403);

  // 7. Cross-user isolation: Alice cannot tamper with Bob's leaderboard entry
  const aliceTamperBob = await fetch(bobUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${aliceToken}` },
    body: JSON.stringify({
      fields: {
        userId: { stringValue: 'bob_uid' },
        displayName: { stringValue: 'Hacked Bob' },
        avatarId: { stringValue: 'avatar_cyber' },
        matchPoints: { integerValue: '1500' },
        matchesPlayed: { integerValue: '3' },
        wins: { integerValue: '2' },
        totalCoinsEarned: { doubleValue: 350.0 },
        favoriteGame: { stringValue: 'LUDO' },
        createdAt: { timestampValue: ts },
        updatedAt: { timestampValue: isoPast(200) },
      },
    }),
  });
  assert.equal(aliceTamperBob.status, 403);

  // 8. Non-admin cannot delete leaderboard entries; Admin can delete
  const aliceDelete = await fetch(aliceUrl, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${aliceToken}` },
  });
  assert.equal(aliceDelete.status, 403);

  const adminDelete = await fetch(bobUrl, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  assert.equal(adminDelete.status, 200, await adminDelete.text());
});



