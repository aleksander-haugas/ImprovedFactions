# Faction Events

The plugin emits several Bukkit events related to faction lifecycle changes. You can listen to them using the standard
Bukkit event system (`@EventHandler`).

### `FactionCreateEvent`

**Triggered:** When a new faction is created.

* **Cancelable:** ✅ Yes
* **Notes:**

    * Canceling this event will prevent the faction from being created.
    * **You must notify the player manually** if the creation is canceled — the plugin does not provide user feedback
      automatically.

---

### `FactionDeleteEvent`

**Triggered:** *Before* a faction is permanently deleted.

* **Cancelable:** ✅ Yes

* **Access:**

    * All faction data (members, name, etc.) is still accessible **during the event**.
    * After the event completes, the faction data will be wiped. Avoid deferring logic using delayed tasks or async
      operations unless you've safely copied the required data.

* **Important:**

    * This is the *only* event triggered on faction deletion.
    * **No `FactionLeaveEvent`s** are fired for departing members.

### `FactionLeaveEvent`

**Triggered:** When a player leaves a faction.

* **Cancelable:** ✅ Yes

* **Timing:**

    * Fired *before* the player is removed from the faction.
    * All relevant player and faction data is still accessible.

* **Important:**

    * This event is **not** fired when a faction is deleted.

### `FactionJoinEvent`

**Triggered:** When a player joins a faction.

* **Cancelable:** ✅ Yes
* **Timing:**

    * Fired *before* the player is added to the faction.
    * The joining player will not yet appear in the faction’s member list at the time of event handling.

### General Notes

* All events are cancelable, but **cancellation requires manual player feedback**.
* Avoid assuming faction data persists after the event lifecycle — especially for deletion-related events.
* Schedule logic cautiously if deferring post-event actions.