# AR Measure App

Measure real-world distances with augmented reality: place points on a
camera view and read the distance between them — in Kotlin for Android.

The measurement core is pure JVM Kotlin with no Android dependency, so it
runs and tests anywhere; on Android it is fed by real ARCore anchor poses
(a `measure.Anchor` is just an id plus a world-space position). The camera
+ AR session layer that produces those poses is the next chunk.

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

## Demo (JVM, no device needed)

```sh
gradle :demo:run --no-daemon
# or: gradle :demo:run --no-daemon --args="--feet"
```

Simulates four anchors on a 4.20 m × 3.10 m room floor and prints the
segments, perimeter (14.60 m), area (13.02 m²), the 90° corner angle, and
a rejected duplicate tap.

## Layout

- `core/` — `measure` package: `Vec3`, `Anchor`, distance/path/angle,
  polygon area/perimeter, units/formatting, `MeasurementSession`
- `demo/` — JVM CLI demo (`measure.demo.MainKt`)

## Roadmap

- [x] Project scaffold (README roadmap, license, gitignore)
- [x] Measurement core (3D point math, distance/area from AR anchors)
- [ ] Camera + AR session (plane detection, point placement)
- [ ] Measurement history + saved measurements
- [ ] Self-review checklist, then public

## License

MIT — see [LICENSE](LICENSE).
