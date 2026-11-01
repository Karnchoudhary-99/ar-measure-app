# AR Measure App

Measure real-world distances with augmented reality: place points on a
camera view and read the distance between them — in Kotlin for Android.

The measurement core is pure JVM Kotlin with no Android dependency, so it
runs and tests anywhere; on Android it is fed by real ARCore anchor poses
(a `measure.Anchor` is just an id plus a world-space position). The camera
+ AR session layer that produces those poses from screen taps is in
place too (`measure.Plane`, `measure.Ray`, `measure.CameraPose`,
`measure.SimulatedArSession`); on Android the simulated session is
swapped for ARCore plane detection and the live camera pose, and nothing
else changes.

## What it does

- **Distance** between two anchors (the tape-measure operation)
- **Path length** through anchors in order
- **Polygon area** via Newell's method — correct for polygons tilted in
  3D, not just axis-aligned rectangles
- **Perimeter** of the closed polygon, **angle** at any corner
- **Units**: metres, centimetres, inches, feet (core always stores
  metres; conversion happens only at display time)
- **Session handling**: duplicate-tap guard (a shaky second tap within
  1 cm of the previous anchor is ignored), undo, one-line summary

## Camera + AR session

- **Plane fitting** from a point cloud: centroid, unit normal, fitted-disc
  extent, plus fit residuals so a non-planar cloud (table edge + floor)
  is visible instead of silently accepted; orientation bands
  (horizontal / vertical / inclined)
- **Tap placement**: a camera pose turns a screen tap into a world ray
  (pinhole model); the nearest plane hit inside its fitted disc wins,
  so a table occludes the wall behind it; a tap past a plane's edge or
  a duplicate of the previous anchor is a miss, not a bad point

## Measurement history + saved measurements

- **SavedMeasurement**: a named, timestamped snapshot of a session's
  anchors, frozen at record time — later session edits (undo, new
  points) never rewrite it. Derived length, perimeter and area are
  recomputed from the snapshot, exactly as the live session computes
  them.
- **MeasurementHistory**: in-memory list, newest first. Recording under
  an existing id replaces it (re-measuring a room updates it, it does
  not duplicate it); remove, rename and case-insensitive search over
  name and note round it out.
- **Persistence**: one versioned JSON file (`MeasurementStore`), no
  third-party dependency. Saves are atomic (temp file + move), a
  missing file loads as an empty history (first run), and a corrupt
  file throws instead of silently wiping saved rooms. On Android this
  store is swapped for on-device storage; the history API does not
  change.

## Demo (JVM, no device needed)

```sh
gradle :demo:run --no-daemon
# or: gradle :demo:run --no-daemon --args="--feet"
# or: gradle :demo:run --no-daemon --args="--ar"
# or: gradle :demo:run --no-daemon --args="--history"
```

Simulates four anchors on a 4.20 m × 3.10 m room floor and prints the
segments, perimeter (14.60 m), area (13.02 m²), the 90° corner angle, and
a rejected duplicate tap. With `--ar`, the same room is measured through
the tap path instead: a camera 3 m above the room centre, a floor plane
fitted from the corners, one tap per corner, plus a tap outside the
floor disc that misses.

## Layout

- `core/` — `measure` package: `Vec3`, `Anchor`, distance/path/angle,
  polygon area/perimeter, units/formatting, `MeasurementSession`,
  `SavedMeasurement`, `MeasurementHistory`, `MeasurementStore`
- `demo/` — JVM CLI demo (`measure.demo.MainKt`)

## Roadmap

- [x] Project scaffold (README roadmap, license, gitignore)
- [x] Measurement core (3D point math, distance/area from AR anchors)
- [x] Camera + AR session (plane detection, point placement)
- [x] Measurement history + saved measurements
- [x] Self-review checklist, then public

## License

MIT — see [LICENSE](LICENSE).
