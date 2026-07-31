#version 150

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float Vanish; // 0 = present, 1 = fully erased

in vec2 texCoord0;

out vec4 fragColor;

// The man behind the tree: a humanoid SDF silhouette warped by drifting noise,
// sheared into slices, filled with crawling static, eroded at the outline

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    return 0.55 * vnoise(p) + 0.30 * vnoise(p * 2.17 + 13.7) + 0.15 * vnoise(p * 4.31 + 71.3);
}

float sdCircle(vec2 p, vec2 c, float r) {
    return length(p - c) - r;
}

float sdSegment(vec2 p, vec2 a, vec2 b, float r) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

void main() {
    float t = GameTime * 24000.0; // ticks

    // body space: x in [-0.5, 0.5], y in [0 feet .. 1 head]
    vec2 p = vec2(texCoord0.x - 0.5, 1.0 - texCoord0.y);

    // reality tearing: some horizontal slices shear sideways for a few ticks
    float sliceId = floor(p.y * 18.0);
    float tear = hash21(vec2(sliceId, floor(t / 4.0)));
    p.x += step(0.92, tear) * (tear - 0.96) * 2.5;

    // the shape never holds still: warp the space the body is built in
    vec2 warp = vec2(
        fbm(p * 3.0 + vec2(t * 0.11, 0.0)),
        fbm(p * 3.0 - vec2(0.0, t * 0.09))) - 0.5;
    vec2 q = p + warp * 0.13;
    // slow, uneasy sway
    q.x += 0.015 * sin(t * 0.05 + q.y * 6.0);

    // humanoid silhouette
    float d = sdCircle(q, vec2(0.0, 0.845), 0.085);                              // head
    d = smin(d, sdSegment(q, vec2(0.0, 0.72), vec2(0.0, 0.34), 0.130), 0.050);   // torso
    d = smin(d, sdSegment(q, vec2(-0.10, 0.66), vec2(-0.20, 0.36), 0.045), 0.035); // arms, hanging
    d = smin(d, sdSegment(q, vec2(0.10, 0.66), vec2(0.20, 0.36), 0.045), 0.035);
    d = smin(d, sdSegment(q, vec2(-0.06, 0.34), vec2(-0.07, 0.02), 0.055), 0.040); // legs
    d = smin(d, sdSegment(q, vec2(0.06, 0.34), vec2(0.07, 0.02), 0.055), 0.040);

    // the boundary itself refuses to hold still; when he leaves, the silhouette
    // erodes inward from the edges until nothing is left
    float edge = 0.012 + 0.045 * fbm(q * 7.0 + t * 0.23);
    float shrink = Vanish * Vanish * 0.30;
    if (d > edge - shrink) {
        discard;
    }

    // interior: near-black void with coarse crawling static
    vec2 cell = floor((p + vec2(0.5, 0.0)) * vec2(22.0, 44.0));
    float s = hash21(cell + floor(t * 1.3));
    vec3 color = vec3(0.02, 0.008, 0.03);
    color += vec3(0.10, 0.02, 0.12) * step(0.82, s); // dark violet flickers
    color += vec3(0.05) * s * 0.3;

    // rare frames where the censor slips and the whole figure lightens
    float slip = step(0.986, hash21(vec2(floor(t / 2.0), 3.7)));
    color += slip * vec3(0.10, 0.12, 0.16);

    // pale fringe where reality is actively erasing the outline; while he
    // dissolves, the fringe crawls across what little remains of him
    float rim = smoothstep(edge - shrink - 0.035, edge - shrink, d);
    float rimFlicker = 0.55 + 0.45 * vnoise(vec2(t * 0.9, q.y * 30.0));
    color = mix(color, vec3(0.78, 0.89, 0.95) * rimFlicker, min(1.0, rim + Vanish * 0.4) * 0.85);

    fragColor = vec4(color, 1.0) * ColorModulator;
}
