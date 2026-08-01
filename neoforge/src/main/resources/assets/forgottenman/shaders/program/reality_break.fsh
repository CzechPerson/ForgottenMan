#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

// Finishing pass while the full mirror hall is open.
//
// The PostChain "Time" uniform wraps 0 to 1 every 20 ticks. Any periodic term has
// to complete whole cycles over that interval or the phase snaps at the wrap;
// everything below is built from cyclic distances, one slow heartbeat per second.

// cyclic distance on the unit circle (continuous across the Time wrap)
float cyc(float x) {
    return fract(x + 0.5) - 0.5;
}

void main() {
    // lub-dub: a strong pulse and a weaker echo, then rest. Wide gaussians so
    // each pulse swells and fades gently instead of snapping.
    float lub = cyc(Time - 0.12);
    float dub = cyc(Time - 0.40);
    float beat = 0.9 * (exp(-lub * lub * 90.0) + 0.55 * exp(-dub * dub * 120.0));

    vec2 centered = texCoord - 0.5;
    float r2 = dot(centered, centered);

    // the view swells gently toward you on each beat
    vec2 uv = texCoord - centered * beat * 0.006;

    // subtle chromatic fringing at the edges, breathing slightly with the pulse
    float aberration = r2 * 0.008 * (1.0 + 0.5 * beat);
    vec3 color;
    color.r = texture(DiffuseSampler, uv + centered * aberration).r;
    color.g = texture(DiffuseSampler, uv).g;
    color.b = texture(DiffuseSampler, uv - centered * aberration).b;

    // steady dark vignette, and a red flush that pulses in from the screen edges
    float vign = smoothstep(0.12, 0.55, r2);
    color *= 1.0 - 0.35 * vign;
    color = mix(color, vec3(0.45, 0.02, 0.04), vign * beat * 0.5);

    fragColor = vec4(color, 1.0);
}
