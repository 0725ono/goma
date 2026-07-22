import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { signInSchema, type SignInValues } from "@/features/auth/model/schema";
import { signIn } from "@/lib/session";

/**
 * ログインフォームのロジック（RHF のセットアップと送信処理）。
 * UI は SignInForm、状態は Zustand セッションに分離している。
 */
export function useSignInForm() {
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<SignInValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit((values) => {
    // 第一段階: 認証は行わず、セッションを立てて (private) へ遷移させる。
    signIn(values);
  });

  return { control, errors, onSubmit };
}
