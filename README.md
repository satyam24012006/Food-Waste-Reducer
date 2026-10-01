# Food Waste Reducer

Spring Boot + WebSocket (STOMP) app where donors (mess, canteen, hostel, home) post surplus
food and receivers (NGOs, students, anyone nearby) claim it. Every change is pushed live to
all open browsers. Donors earn reward points when their food is actually picked up, and can
redeem those points for badges/coupons.

## Run

Needs JDK 17+ and Maven 3.9+ (or open the folder in IntelliJ and run
`FoodWasteReducerApplication`).

    mvn spring-boot:run

Open http://localhost:8080

Default admin (change in `application.properties`): `admin@foodwaste.com` / `admin123`
(the admin account can delete any listing but cannot post or claim food).

Database defaults to a local H2 file (zero setup). To use MySQL instead, edit
`src/main/resources/application.properties` — the commented block has the exact lines to
swap in, and reads the password from the `DB_PASSWORD` environment variable so it is never
committed to source control.

## What's in this version

**Auth & roles**
- JWT auth, roles `DONOR` / `RECEIVER` / `ADMIN`.
- Register requires name, email, 10-digit phone, password, and a role.
- `AccessDeniedException` is handled explicitly so a wrong-role request returns **403**,
  not a raw 500.

**Role-based dashboards (this is new)**
- **Donor** sees: the post-food form, an *Impact & Rewards* panel (points + counts), and
  *My Posted Food* — only their own listings. Donors never see the full public
  "available food" browsing list — that's a receiver's job.
- **Receiver** sees: an *Expiring Soon* banner at the top (available food expiring within
  the next hour, soonest first), and a tabbed *Food Near You* list — **Available** /
  **My Claims** / **History**.

**Listings**
- Post, claim, cancel claim, mark picked up, delete.
- Optimistic locking (`@Version`) — two receivers can't claim the same listing at once;
  the loser gets a clean 409, not a crash.
- A scheduler runs every minute and expires overdue `AVAILABLE` **and** `CLAIMED`
  listings (claiming something doesn't stop the clock).
- Donor and receiver location (lat/long) is captured via the browser's geolocation API
  and stored per listing — the plumbing a "within N km" filter would build on next.

**Contact reveal**
- Phone numbers are never broadcast. Once a listing is claimed, only that donor and that
  receiver can call `GET /api/listings/{id}/contact` to see each other's name and number.

**Rewards**
- Donors earn 1 point per serving *picked up* (configurable via
  `app.rewards.points-per-serving`) — not per post, so empty/fake listings can't farm points.
- A small seeded catalog (bronze/silver/gold badges, a coupon, a certificate) is redeemable
  once a donor has enough points; redeeming deducts points and returns a coupon code.
- Redeeming is tied to the logged-in user's token — you can only redeem for yourself.

**Real-time**
- WebSocket (STOMP) at `/ws`, topic `/topic/listings`. Every post/claim/cancel/pickup/
  delete/expiry is broadcast, and the browser refreshes only the relevant dashboard
  (donor screen or receiver screen) without a manual reload.

## REST API

| Method | Path | Who | What |
|---|---|---|---|
| POST | /api/auth/register | public | create account, returns JWT |
| POST | /api/auth/login | public | returns JWT |
| GET | /api/listings | public | all listings, newest first |
| GET | /api/listings/expiring-soon | public | available listings expiring soon |
| GET | /api/listings/mine | DONOR | the caller's own listings |
| POST | /api/listings | DONOR | post food |
| POST | /api/listings/{id}/claim | RECEIVER | claim available food |
| POST | /api/listings/{id}/cancel | RECEIVER (who claimed) | release the claim |
| POST | /api/listings/{id}/pickup | DONOR (owner) | confirm pickup, awards points |
| DELETE | /api/listings/{id} | DONOR (owner, if available) / ADMIN | delete listing |
| GET | /api/listings/{id}/contact | donor or claiming receiver | reveal the other party's contact |
| GET | /api/stats | public | site-wide counts |
| GET | /api/donor/stats | DONOR | the caller's own points + counts |
| GET | /api/rewards | public | reward catalog |
| POST | /api/rewards/redeem | DONOR | redeem a reward for the caller |
| GET | /api/rewards/mine | DONOR | the caller's redemption history |

Send the token as `Authorization: Bearer <token>`.

## Still open (by design, not yet built)

- **Radius filtering** — lat/long is captured and stored, but listings aren't yet filtered
  by distance from the receiver. Add a Haversine-distance query once the receiver's own
  location is captured the same way.
- **Volunteer role / delivery hand-off** — not built. Would add a `VOLUNTEER` role and a
  `CLAIMED → ASSIGNED → PICKED_UP → DELIVERED` flow.
- **Automated tests** — only the default `contextLoads()` smoke test exists.

## Before deploying

Change `app.jwt.secret` and the admin password, move both (and the DB password) to
environment variables, and restrict `setAllowedOriginPatterns` in `WebSocketConfig` to your
real domain.

## Note on testing

This project was written and reviewed carefully, including matching every DTO field order,
repository method name, and endpoint the frontend calls — but it has **not** been run
through an actual Maven build in this environment (no network access to Maven Central here).
Please run `mvn spring-boot:run` and share the exact error text if anything fails to compile
or start — most issues at this stage tend to be one typo away from fixed.
