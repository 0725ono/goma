import { StyleSheet, Text, TouchableOpacity } from "react-native";

import { signOut } from "@/lib/session";

/**
 * サインアウトのボタン。session（基盤）の signOut を呼ぶだけ。
 * 消去などの副作用は signOut 側に集約し、この UI は知らない。
 */
export function SignOutButton() {
  return (
    <TouchableOpacity style={styles.button} onPress={() => signOut()}>
      <Text style={styles.text}>サインアウト</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: "#D32F2F",
  },
  text: { color: "#D32F2F", fontSize: 13, fontWeight: "bold" },
});
